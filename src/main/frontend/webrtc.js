let pc = null;
let ws = null;
let wsOpen = false;
let wsQueue = [];
let iceQueue = [];
let madeOffer = false;
let isInitiator = false;

function wsSend(obj) {
    console.log("[WS send]", obj);
    if (wsOpen) {
        try {
            ws.send(JSON.stringify(obj));
        } catch (e) {
            console.warn("[WS] send failed:", e);
            wsQueue.push(obj);
        }
    } else {
        wsQueue.push(obj);
    }
}

function flushWsQueue() {
    if (!wsOpen) return;
    while (wsQueue.length) {
        const msg = wsQueue.shift();
        try { ws.send(JSON.stringify(msg)); } catch (e) { console.warn("[WS] flush send failed", e); wsQueue.unshift(msg); break; }
    }
}

function flushIceQueue() {
    if (!pc) return;
    if (!pc.remoteDescription || !pc.remoteDescription.type) {
        console.log("[ICE] remoteDescription not set yet; keep queue size:", iceQueue.length);
        return;
    }
    while (iceQueue.length) {
        const candidate = iceQueue.shift();
        pc.addIceCandidate(candidate).catch(e => console.warn("[ICE] addIceCandidate failed from queue", e));
    }
}

// ===== публичные функции вызываемые из Vaadin =====
window.startCall = async function () {
    isInitiator = true;
    await setupWebRTC();
};

window.answerCall = async function () {
    isInitiator = false;
    await setupWebRTC();
};

// ================== основная логика ==================
async function setupWebRTC() {
    if (pc) {
        console.warn("[webrtc] pc already exists — reusing (closing and recreating)");
        try { pc.close(); } catch(e) {}
        pc = null;
        iceQueue = [];
        wsQueue = [];
        madeOffer = false;
    }

    pc = new RTCPeerConnection({
        iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });

    pc.onicecandidate = (e) => {
        if (e.candidate) {
            // Отправляем candidate или кладём в очередь, wsSend сам буферизует, но candidate может прийти раньше wsOpen
            wsSend({ ice: e.candidate });
        }
    };

    pc.oniceconnectionstatechange = () => {
        console.log("[pc] iceConnectionState:", pc.iceConnectionState);
    };

    pc.onconnectionstatechange = () => {
        console.log("[pc] connectionState:", pc.connectionState);
    };

    pc.ontrack = (e) => {
        console.log("[pc] ontrack", e.streams);
        attachVideo("remoteVideo", e.streams[0], false); // remote не muted
    };

    // получаем медиа (локальное)
    let stream;
    try {
        stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    } catch (err) {
        console.error("[webrtc] getUserMedia failed:", err);
        alert("Не удалось получить доступ к камере/микрофону: " + err.message);
        return;
    }

    attachVideo("localVideo", stream, true);
    stream.getTracks().forEach(track => pc.addTrack(track, stream));

    ws = new WebSocket("wss://5mwqpj9k-8080.euw.devtunnels.ms/signal");
    wsOpen = false;

    ws.onopen = () => {
        console.log("[WS] open");
        wsOpen = true;
        flushWsQueue();
        // после открытия ws, если мы инициатор и ещё не сделали offer — делаем его
        if (isInitiator && !madeOffer) {
            createAndSendOffer().catch(e => console.error("[webrtc] createOffer error", e));
        }
    };

    ws.onclose = (ev) => {
        console.log("[WS] close", ev);
        wsOpen = false;
    };

    ws.onerror = (err) => {
        console.warn("[WS] error", err);
    };

    ws.onmessage = async (evt) => {
        let data;
        try {
            data = JSON.parse(evt.data);
        } catch (e) {
            console.warn("[WS] failed parse message", evt.data);
            return;
        }
        console.log("[WS recv]", data);

        // ICE
        if (data.ice) {
            // если remoteDescription ещё не установлен — буферизуем кандидат
            if (!pc.remoteDescription || !pc.remoteDescription.type) {
                iceQueue.push(data.ice);
                console.log("[ICE] queued, queue length =", iceQueue.length);
            } else {
                try {
                    await pc.addIceCandidate(data.ice);
                    console.log("[ICE] added candidate");
                } catch (e) {
                    console.warn("[ICE] addIceCandidate failed", e);
                }
            }
            return;
        }

        // OFFER
        if (data.offer) {
            console.log("[SDP] received offer");
            try {
                // если мы уже создали offer и еще не установили remote, это glare — простой разрешатель:
                // если мы сделали offer (madeOffer === true) и получили offer от peer, корректнее — отказаться от своей роли и принять чужой.
                if (madeOffer && isInitiator) {
                    console.log("[glare] we made offer but received offer — backing off to accept remote");
                    // закрыть старое local desc? проще: позволяем setRemoteDescription перезаписать при actpass/activesetup handled by browser
                }

                await pc.setRemoteDescription(data.offer);
                // применить накопленные кандидаты
                flushIceQueue();

                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);
                wsSend({ answer });
                console.log("[SDP] sent answer");
            } catch (e) {
                console.error("[SDP] processing offer failed", e);
            }
            return;
        }

        // ANSWER
        if (data.answer) {
            console.log("[SDP] received answer");
            try {
                await pc.setRemoteDescription(data.answer);
                console.log("[SDP] remoteDescription set from answer");
                // после установки remote можно применить буферные кандидаты
                flushIceQueue();
            } catch (e) {
                console.error("[SDP] setRemoteDescription(answer) failed", e);
            }
            return;
        }
    };

    // Дополнительная страховка: если ws не открылся, но мы всё равно хотим инициировать (например startCall вызван до открытия ws),
    // createAndSendOffer будет выполнен после ws.onopen -> flushWsQueue(), либо можно вызвать тут с небольшым таймаут.
}

async function createAndSendOffer() {
    if (!pc) throw new Error("pc is not created");
    try {
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        madeOffer = true;
        wsSend({ offer });
        console.log("[SDP] offer created and sent");
    } catch (e) {
        console.error("[SDP] createOffer failed", e);
    }
}

// ===================== UI helper =====================
function attachVideo(id, stream, muted = false) {
    const container = document.getElementById(id);
    if (!container) {
        console.warn("[attachVideo] container not found:", id);
        return;
    }
    container.innerHTML = "";
    const video = document.createElement("video");
    video.srcObject = stream;
    video.autoplay = true;
    video.playsInline = true;
    video.muted = !!muted;

    container.appendChild(video);
}