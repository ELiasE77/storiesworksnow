const speechToTextBtn = document.getElementById('speechToTextBtn');
const journalContent = document.getElementById('journal-content');
const speechIndicator = document.getElementById('speechIndicator');

// Check if the browser supports Speech Recognition
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (SpeechRecognition) {
    const recognition = new SpeechRecognition();

    recognition.continuous = false; // Stop recognition after one result
    recognition.interimResults = false; // We want final results only

    recognition.onstart = () => {
        speechIndicator.style.display = 'block'; // Show listening indicator
    };

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript; // Get the recognized text
        journalContent.value += (transcript + ' '); // Append it to the textarea
        speechIndicator.style.display = 'none'; // Hide the indicator
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        speechIndicator.style.display = 'none'; // Hide the indicator on error
    };

    recognition.onend = () => {
        speechIndicator.style.display = 'none'; // Hide the indicator when recognition ends
    };

    speechToTextBtn.addEventListener('click', () => {
        recognition.start();
    });
} else {
    console.warn('Speech Recognition is not supported in this browser.');
    speechToTextBtn.disabled = true; // Disable the button if not supported
}