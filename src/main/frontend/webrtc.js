let pc;
let ws;
let wsOpen = false;
let wsQueue = [];

function wsSend(data) {
    if (wsOpen) {
        ws.send(JSON.stringify(data));
    } else {
        wsQueue.push(data);
    }
}

window.startCall = async () => {

    ws = new WebSocket("ws://localhost:8080/signal");

    ws.onopen = () => {
        wsOpen = true;
        wsQueue.forEach(msg => ws.send(JSON.stringify(msg)));
        wsQueue = [];
    };

    ws.onmessage = async (msg) => {
        const data = JSON.parse(msg.data);

        if (data.offer) {
            await pc.setRemoteDescription(data.offer);
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            wsSend({ answer });
        }

        if (data.answer) {
            await pc.setRemoteDescription(data.answer);
        }

        if (data.ice) {
            await pc.addIceCandidate(data.ice);
        }
    };

    pc = new RTCPeerConnection({
        iceServers: [
            { urls: "stun:stun.l.google.com:19302" }
        ]
    });

    pc.onicecandidate = e => {
        if (e.candidate) wsSend({ ice: e.candidate });
    };

    pc.ontrack = e => {
        attachVideo("remoteVideo", e.streams[0]);
    };

    // Получаем камеру/микрофон
    const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
    });

    attachVideo("localVideo", stream);

    stream.getTracks().forEach(t => pc.addTrack(t, stream));

    // Создаем предложение
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);

    wsSend({ offer });
};

function attachVideo(id, stream) {
    const block = document.getElementById(id);
    block.innerHTML = "";
    const v = document.createElement("video");
    v.autoplay = true;
    v.playsinline = true;
    v.srcObject = stream;
    block.appendChild(v);
}