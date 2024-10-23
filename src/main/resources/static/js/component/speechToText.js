const speechToTextBtn = document.getElementById('speechToTextBtn');
const journalContent = document.getElementById('journal-content');
const speechIndicator = document.getElementById('speechIndicator');

// Check if the browser supports Speech Recognition
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (SpeechRecognition) {
    const recognition = new SpeechRecognition();

    recognition.continuous = true; // Stop recognition after one result
    recognition.interimResults = false; // We want final results only
    let isListening = false;

    recognition.onstart = () => {
        speechIndicator.style.display = 'block'; // Show listening indicator
        isListening = true; // Set listening status to true
        speechToTextBtn.textContent = 'Stop Listening'; // Change button text
    };

    recognition.onresult = (event) => {
        const transcript = event.results[event.resultIndex][0].transcript; // Get the recognized text
        journalContent.value += transcript; // Append it to the textarea
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        speechIndicator.style.display = 'none'; // Hide the indicator on error
        isListening = false;
        speechToTextBtn.textContent = 'Start Speech-to-Text'; // Reset button text

    };

    recognition.onend = () => {
        speechIndicator.style.display = 'none'; // Hide the indicator when recognition ends
        isListening = false; // Set listening status to false
        speechToTextBtn.textContent = 'Start Speech-to-Text'; // Reset button text
    };

    // Toggle start/stop of recognition
    speechToTextBtn.addEventListener('click', () => {
        if (isListening) {
            recognition.stop(); // Stop recognition if it’s currently active
        } else {
            recognition.start(); // Start recognition if it’s currently inactive
        }
    });
} else {
    console.warn('Speech Recognition is not supported in this browser.');
    speechToTextBtn.disabled = true; // Disable the button if not supported
}