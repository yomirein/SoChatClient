let stompClient = null;
let pc = null;
let localStream = null;
let remoteStream = null;
let connectedUserId = null;
let pendingCandidates = [];
let headers = {
    Authorization: "Bearer " + "",
    cookie: document.cookie
};
const APP_PREFIX = "/app";
const USER_QUEUE = "/user/queue/call";

const rtcConfig = {
    iceServers: [
        { urls: ["stun:stun.l.google.com:19302"] }
    ]
};

function setStatus(text) {
    const el = document.getElementById("connectionStatus");
    if (el) el.textContent = text;
}

window.ensureStomp = function ensureStomp() {
    if (stompClient && stompClient.connected) return;
    const token = window.authToken;
    headers = {
        Authorization: "Bearer " + token,
        cookie: document.cookie
    };
    console.log(headers)

    const socket = new SockJS("https://localhost:8443/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = () => {};

    console.log("GOT TOKEN IN JS " + token)

    stompClient.connect(
        { Authorization: "Bearer " + token },
        () => {
            setStatus("STOMP соединение установлено.");
            stompClient.subscribe(USER_QUEUE, onSignalMessage, headers);
        },
        (err) => {
            setStatus("Ошибка STOMP: " + err);
        }
    );

};

window.startCallTo = async function startCallTo(targetUserId) {
    try {
        if (pc) {
            setStatus("Уже идёт звонок. Сначала завершите текущий.");
            return;
        }
        connectedUserId = targetUserId;

        await setupPeerConnection();

        const offer = await pc.createOffer({
            offerToReceiveAudio: false,
            offerToReceiveVideo: true,
        });
        await pc.setLocalDescription(offer);

        sendSignal("/call/offer", {
            toUserId: targetUserId,
            fromUserId: window.__MY_USER_ID,
            type: "offer",
            status: "ringing",
            sdp: offer.sdp
        });

        setStatus("Отправлен запрос на звонок...");
    } catch (e) {
        console.error(e);
        setStatus("Не удалось начать звонок: " + e.message);
        cleanup();
    }
};

window.endCall = function endCall() {
    if (connectedUserId) {
        sendSignal("/call/control", {
            toUserId: connectedUserId,
            fromUserId: window.__MY_USER_ID,
            type: "control",
            status: "end"
        });
    }
    cleanup();
    setStatus("Звонок завершён.");
};

window.toggleVideo = function toggleVideo() {
    if (!localStream) return;
    const videoTrack = localStream.getVideoTracks()[0];
    if (!videoTrack) return;
    videoTrack.enabled = !videoTrack.enabled;
};

window.toggleMic = function toggleMic() {
    if (!localStream) return;
    const audioTrack = localStream.getAudioTracks()[0];
    if (!audioTrack) return;
    audioTrack.enabled = !audioTrack.enabled;
};

// Новый метод: принять звонок
window.acceptIncomingCall = async function acceptIncomingCall() {
    try {
        if (!pc) await setupPeerConnection();

        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);

        sendSignal("/call/answer", {
            toUserId: connectedUserId,
            fromUserId: window.__MY_USER_ID,
            type: "answer",
            status: "accepted",
            sdp: answer.sdp
        });

        setStatus("Вы приняли звонок. Отправлен ответ.");
    } catch (e) {
        console.error(e);
        setStatus("Ошибка при принятии звонка: " + e.message);
        cleanup();
    }
};

// Новый метод: отклонить звонок
window.rejectIncomingCall = function rejectIncomingCall() {
    if (connectedUserId) {
        sendSignal("/call/control", {
            toUserId: connectedUserId,
            fromUserId: window.__MY_USER_ID,
            type: "control",
            status: "rejected"
        });
    }
    cleanup();
    setStatus("Звонок отклонён.");
};

function onSignalMessage(frame) {
    let msg;
    try {
        msg = JSON.parse(frame.body);
    } catch (e) {
        console.warn("Bad signal message", frame.body);
        return;
    }

    const { type, status, sdp, ice, fromUserId } = msg;

    if (!connectedUserId && fromUserId) {
        connectedUserId = fromUserId;
    }
    switch (type) {
        case "offer":
            // Caller не должен обрабатывать чужие offer
            if (fromUserId !== window.__MY_USER_ID) {
                handleOffer(msg);
            }
            break;
        case "answer":
            // callee не должен обрабатывать свой же answer
            if (fromUserId !== window.__MY_USER_ID) {
                handleAnswer(sdp);
            }
            break;
        case "ice":
            handleRemoteIce(ice);
            break;
        case "control":
            handleControl(status);
            break;
    }
}

async function handleOffer(msg) {
    try {
        if (!pc) await setupPeerConnection();
        pendingCandidates = [];
        await pc.setRemoteDescription({
            type: "offer",
            sdp: msg.sdp
        });

        connectedUserId = msg.fromUserId;

        setStatus("Входящий звонок от пользователя " + msg.fromUserId + ". Нажмите Join Call для ответа.");
    } catch (e) {
        console.error(e);
        setStatus("Ошибка обработки предложения: " + e.message);
        sendSignal("/call/control", {
            toUserId: msg.fromUserId,
            fromUserId: window.__MY_USER_ID,
            type: "control",
            status: "rejected"
        });
        cleanup();
    }
}

async function handleAnswer(sdp) {
    try {
        if (!pc) {
            console.warn("No peer connection for answer");
            return;
        }
        await pc.setRemoteDescription({ type: "answer", sdp });
        setStatus("Удалённое SDP установлено. Соединение устанавливается...");

        // теперь можно добавить все накопленные ICE кандидаты
        pendingCandidates.forEach(c => pc.addIceCandidate(c));
        pendingCandidates = [];
    } catch (e) {
        console.error(e);
        setStatus("Ошибка установки ответа: " + e.message);
        cleanup();
    }
}

// Приходит ICE от удалённого
function handleRemoteIce(iceJson) {
    try {
        const candidate = JSON.parse(iceJson);
        if (pc && pc.remoteDescription && pc.remoteDescription.type) {
            pc.addIceCandidate(candidate).catch(err => {
                console.warn("ICE add failed", err);
            });
        } else {
            // пока remoteDescription не установлен — складываем в очередь
            pendingCandidates.push(candidate);
        }
    } catch (e) {
        console.warn("Bad ICE", iceJson, e);
    }
}

function handleControl(status) {
    if (status === "end" || status === "rejected") {
        setStatus(status === "end" ? "Партнёр завершил звонок." : "Звонок отклонён.");
        cleanup();
    } else if (status === "busy") {
        setStatus("Пользователь занят.");
        cleanup();
    }
}

async function setupPeerConnection() {
    pc = new RTCPeerConnection(rtcConfig);

    localStream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true });
    attachStream("localVideo", localStream);

    localStream.getTracks().forEach(track => pc.addTrack(track, localStream));

    remoteStream = new MediaStream();
    attachStream("remoteVideo", remoteStream);

    pc.ontrack = (event) => {
        event.streams[0].getTracks().forEach(track => remoteStream.addTrack(track));
    };

    pc.onicecandidate = (event) => {
        if (event.candidate && connectedUserId) {
            sendSignal("/call/ice", {
                toUserId: connectedUserId,
                fromUserId: window.__MY_USER_ID,
                type: "ice",
                ice: JSON.stringify(event.candidate)
            });
        }
    };

    pc.onconnectionstatechange = () => {
        setStatus("Состояние соединения: " + pc.connectionState);
        if (pc.connectionState === "failed" || pc.connectionState === "disconnected" || pc.connectionState === "closed") {
            if (pc.connectionState === "failed") cleanup();
        }
    };
}

function attachStream(containerId, stream) {
    const container = document.getElementById(containerId);
    if (!container) return;

    let video = container.querySelector("video");
    if (!video) {
        video = document.createElement("video");
        video.autoplay = true;
        video.playsInline = true;
        if (containerId === "localVideo") video.muted = true;
        video.style.width = "100%";
        video.style.height = "auto";
        container.appendChild(video);
    }
    video.srcObject = stream;
}

function sendSignal(path, payload) {
    if (!stompClient || !stompClient.connected) {
        setStatus("STOMP не готов. Переподключение...");
        window.ensureStomp();
        return;
    }
    stompClient.send(APP_PREFIX + path, headers, JSON.stringify(payload));
}
function cleanup() {
    connectedUserId = null;
    pendingCandidates = [];

    if (pc) {
        try { pc.onicecandidate = null; pc.ontrack = null; pc.close(); } catch (e) {}
        pc = null;
    }
    if (localStream) {
        localStream.getTracks().forEach(t => t.stop());
        localStream = null;
    }
    if (remoteStream) {
        remoteStream.getTracks().forEach(t => t.stop());
        remoteStream = null;
    }

    // Clear video elements
    const lv = document.querySelector("#localVideo video");
    const rv = document.querySelector("#remoteVideo video");
    if (lv) lv.srcObject = null;
    if (rv) rv.srcObject = null;
}