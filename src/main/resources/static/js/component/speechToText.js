const speechToTextBtn = document.getElementById('speechToTextBtn');
const targetFieldId = speechToTextBtn?.dataset?.target || 'journal-content';
const transcriptFieldId = speechToTextBtn?.dataset?.transcriptTarget || targetFieldId;
const audioTargetId = speechToTextBtn?.dataset?.audioTarget || '';
const shouldRecordAudio = speechToTextBtn?.dataset?.recordAudio === 'true';
const journalContent = document.getElementById(targetFieldId);
const transcriptField = document.getElementById(transcriptFieldId);
const audioDataField = audioTargetId ? document.getElementById(audioTargetId) : null;
const speechIndicator = document.getElementById('speechIndicator');
const voicePlayback = document.getElementById('voiceMemoPlayback');
const speechI18n = window.journallyI18n || {};
const speechMsg = (key, fallback) => speechI18n[key] || fallback;

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
const canRecordAudio = Boolean(navigator.mediaDevices?.getUserMedia && window.MediaRecorder);

if (SpeechRecognition && speechToTextBtn && journalContent && speechIndicator) {
    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;

    let isListening = false;
    let mediaStream = null;
    let mediaRecorder = null;
    let audioChunks = [];
    let baseText = '';
    let finalTranscript = '';

    function joinTranscript(base, liveText) {
        const normalizedBase = base.trimEnd();
        const normalizedLive = liveText.trimStart();
        if (!normalizedBase) {
            return normalizedLive;
        }
        if (!normalizedLive) {
            return normalizedBase;
        }
        return `${normalizedBase} ${normalizedLive}`;
    }

    function syncTranscript(value) {
        if (transcriptField && transcriptField !== journalContent) {
            transcriptField.value = value;
        }
        journalContent.dispatchEvent(new Event('input', { bubbles: true }));
    }

    async function startRecording() {
        if (!shouldRecordAudio || !canRecordAudio) {
            return;
        }

        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const preferredType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
                ? 'audio/webm;codecs=opus'
                : 'audio/webm';
        mediaRecorder = new MediaRecorder(mediaStream, { mimeType: preferredType });
        audioChunks = [];

        mediaRecorder.addEventListener('dataavailable', (event) => {
            if (event.data && event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener('stop', () => {
            const mimeType = mediaRecorder?.mimeType || 'audio/webm';
            const blob = new Blob(audioChunks, { type: mimeType });
            if (voicePlayback && blob.size > 0) {
                voicePlayback.src = URL.createObjectURL(blob);
                voicePlayback.hidden = false;
            }
            if (audioDataField && blob.size > 0) {
                const reader = new FileReader();
                reader.onload = () => {
                    audioDataField.value = (reader.result || '').toString();
                };
                reader.readAsDataURL(blob);
            }
            mediaStream?.getTracks().forEach((track) => track.stop());
            mediaStream = null;
            mediaRecorder = null;
        });

        mediaRecorder.start();
    }

    function stopRecording() {
        if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.stop();
        } else {
            mediaStream?.getTracks().forEach((track) => track.stop());
            mediaStream = null;
        }
    }

    recognition.onstart = () => {
        speechIndicator.style.display = 'block';
        isListening = true;
        baseText = journalContent.value || '';
        finalTranscript = '';
        speechToTextBtn.textContent = speechMsg('speechStop', 'Stop listening');
    };

    recognition.onresult = (event) => {
        let interimTranscript = '';
        for (let index = event.resultIndex; index < event.results.length; index++) {
            const transcript = event.results[index][0].transcript;
            if (event.results[index].isFinal) {
                finalTranscript += `${transcript} `;
            } else {
                interimTranscript += transcript;
            }
        }

        const liveText = `${finalTranscript}${interimTranscript}`.trim();
        journalContent.value = joinTranscript(baseText, liveText);
        syncTranscript(journalContent.value);
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        stopRecording();
        speechIndicator.style.display = 'none';
        isListening = false;
        speechToTextBtn.textContent = speechMsg('speechStart', 'Start speech-to-text');
    };

    recognition.onend = () => {
        stopRecording();
        speechIndicator.style.display = 'none';
        isListening = false;
        speechToTextBtn.textContent = speechMsg('speechStart', 'Start speech-to-text');
        syncTranscript(journalContent.value || '');
    };

    speechToTextBtn.addEventListener('click', async () => {
        if (isListening) {
            recognition.stop();
            return;
        }

        try {
            await startRecording();
            recognition.start();
        } catch (error) {
            console.error('Could not start audio recording:', error);
            stopRecording();
            recognition.start();
        }
    });
} else {
    console.warn('Speech Recognition is not supported in this browser.');
    if (speechToTextBtn) {
        speechToTextBtn.disabled = true;
        speechToTextBtn.textContent = speechMsg('speechUnsupported', 'Speech recognition unavailable');
    }
}
