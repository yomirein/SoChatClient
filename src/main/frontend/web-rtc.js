let stompClient = null;
let pc = null;
let localStream = null;
let remoteStream = null;
let connectedUserId = null;

const APP_PREFIX = "/app";     // application destination prefix
const USER_QUEUE = "/user/queue/call"; // user-specific queue for incoming signaling

// Your TURN/STUN servers; update to your infra
const rtcConfig = {
    iceServers: [
        { urls: ["stun:stun.l.google.com:19302"] },
        // Example TURN (replace with your own):
        // { urls: "turn:your.turn.server:3478", username: "user", credential: "pass" }
    ]
};

// Utility: log to a status element if present
function setStatus(text) {
    const el = document.getElementById("connectionStatus");
    if (el) el.textContent = text;
}

// Boot STOMP and subscribe to user queue
window.ensureStomp = function ensureStomp() {
    if (stompClient && stompClient.connected) return;

    // SockJS for compatibility with Spring's withSockJS()
    const socket = new SockJS("http://localhost:8080/ws"); //тест
    stompClient = Stomp.over(socket);

    // Optional: silence debug logs
    stompClient.debug = () => {};

    stompClient.connect({}, () => {
        setStatus("STOMP соединение установлено.");
        stompClient.subscribe("/topic/call", onSignalMessage);
    }, (err) => {
        setStatus("Ошибка STOMP: " + err);
    });
};

// Outgoing call: create RTCPeerConnection, local media, send offer
window.startCallTo = async function startCallTo(targetUserId) {
    try {
        if (pc) {
            setStatus("Уже идёт звонок. Сначала завершите текущий.");
            return;
        }
        connectedUserId = targetUserId;

        await setupPeerConnection();

        const offer = await pc.createOffer({
            offerToReceiveAudio: true,
            offerToReceiveVideo: true,
        });
        await pc.setLocalDescription(offer);

        // Send offer via STOMP
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

// End the active call
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

// Toggle local video track
window.toggleVideo = function toggleVideo() {
    if (!localStream) return;
    const videoTrack = localStream.getVideoTracks()[0];
    if (!videoTrack) return;
    videoTrack.enabled = !videoTrack.enabled;
};

// Toggle local audio track (mic)
window.toggleMic = function toggleMic() {
    if (!localStream) return;
    const audioTrack = localStream.getAudioTracks()[0];
    if (!audioTrack) return;
    audioTrack.enabled = !audioTrack.enabled;
};

// Принимаем входящий звонок (callee)
window.acceptIncomingCall = async function acceptIncomingCall() {
    try {
        if (!pc) await setupPeerConnection();

        // remoteDescription уже должен быть установлен в handleOffer
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

// Отклонить звонок
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

// Handle incoming STOMP messages
function onSignalMessage(frame) {
    let msg;
    try {
        msg = JSON.parse(frame.body);
    } catch (e) {
        console.warn("Bad signal message", frame.body);
        return;
    }

    const { type, status, sdp, ice, fromUserId } = msg;

    // Remember counterpart
    if (!connectedUserId && fromUserId) {
        connectedUserId = fromUserId;
    }

    switch (type) {
        case "offer":
            handleOffer(msg);
            break;
        case "answer":
            handleAnswer(sdp);
            break;
        case "ice":
            handleRemoteIce(ice);
            break;
        case "control":
            handleControl(status);
            break;
        default:
            console.log("Unknown signal type", msg);
    }
}

async function handleOffer(msg) {
    try {
        if (!pc) await setupPeerConnection();

        await pc.setRemoteDescription({
            type: "offer",
            sdp: msg.sdp
        });

        connectedUserId = msg.fromUserId;

        // Вместо автоматического ответа — ждём нажатия кнопки Join Call
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
    } catch (e) {
        console.error(e);
        setStatus("Ошибка установки ответа: " + e.message);
        cleanup();
    }
}

function handleRemoteIce(iceJson) {
    try {
        if (!pc) return;
        const candidate = JSON.parse(iceJson);
        pc.addIceCandidate(candidate).catch(err => {
            console.warn("ICE add failed", err);
        });
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

// Create RTCPeerConnection and media
async function setupPeerConnection() {
    pc = new RTCPeerConnection(rtcConfig);

    // Local media
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });

    // Attach local stream to UI
    attachStream("localVideo", localStream);

    // Add tracks to peer connection
    localStream.getTracks().forEach(track => pc.addTrack(track, localStream));

    // Remote stream
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
            // We keep cleanup on 'end' for UX; but if it fails, clean anyway
            if (pc.connectionState === "failed") cleanup();
        }
    };
}

// Helper: attach stream to container (creates <video> inside your Vaadin VerticalLayout)
function attachStream(containerId, stream) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Reuse or create video element
    let video = container.querySelector("video");
    if (!video) {
        video = document.createElement("video");
        video.autoplay = true;
        video.playsInline = true;
        // local video is muted to avoid echo
        if (containerId === "localVideo") video.muted = true;
        video.style.width = "100%";
        video.style.height = "auto";
        container.appendChild(video);
    }
    video.srcObject = stream;
}

// Send signaling via STOMP
function sendSignal(path, payload) {
    if (!stompClient || !stompClient.connected) {
        setStatus("STOMP не готов. Переподключение...");
        window.ensureStomp();
        return;
    }
    stompClient.send(APP_PREFIX + path, {}, JSON.stringify(payload));
}

// Teardown everything cleanly
function cleanup() {
    connectedUserId = null;

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