document.addEventListener('DOMContentLoaded', () => {
    const i18n = window.journallyI18n || {};
    const msg = (key, fallback) => i18n[key] || fallback;
    const generateButton = document.getElementById('generate-image');
    const loadingIndicator = document.getElementById('loading-indicator');
    const imageUrlInput = document.getElementById('imageUrl');
    const generatedImage = document.getElementById('generated-image');
    const generatedImageContainer = document.getElementById('generated-image-container');
    const imageUrlsJsonInput = document.getElementById('imageUrlsJson');

    if (!generateButton) {
        return;
    }

    function toImageSrc(imageData) {
        if (!imageData) {
            return '';
        }
        if (imageData.startsWith('data:image')
            || imageData.startsWith('/uploads/')
            || imageData.startsWith('/images/')
            || imageData.startsWith('http://')
            || imageData.startsWith('https://')) {
            return imageData;
        }
        return `data:image/png;base64,${imageData}`;
    }

    generateButton.addEventListener('click', async () => {
        const journalText = document.getElementById('journal-content')?.value;
        const selectedStyle = document.getElementById('selected-style')?.value || 'realistic';
        const contextImages = Array.isArray(window.userContextImages) ? window.userContextImages : [];
        const visualMemoryAnswer = typeof window.collectEntryData === 'function'
                ? (window.collectEntryData().reflectionPrompts || []).find((prompt) => prompt.kind === 'VISUAL_MEMORY')?.answer || ''
                : '';

        if (!journalText) {
            alert(msg('writeFirstAlert', 'Please write something in the journal first.'));
            return;
        }

        const feedbackPromise = typeof window.generateJournalFeedback === 'function'
                ? window.generateJournalFeedback({ quiet: true }).catch((error) => console.error('Feedback generation failed:', error))
                : Promise.resolve();

        loadingIndicator.style.display = 'block';
        generateButton.disabled = true;

        try {
            const base64Image = await generateImageFromJournalEntry(journalText, selectedStyle, visualMemoryAnswer, contextImages);
            const normalizedImage = toImageSrc(base64Image);

            if (generatedImage) {
                generatedImage.src = normalizedImage;
                generatedImage.style.display = 'block';
            }
            if (generatedImageContainer) {
                generatedImageContainer.style.display = 'grid';
            }

            if (imageUrlsJsonInput) {
                let current = [];
                try {
                    current = JSON.parse(imageUrlsJsonInput.value || '[]');
                } catch (error) {
                    current = [];
                }

                if (!current.includes(normalizedImage)) {
                    current.unshift(normalizedImage);
                }
                if (Array.isArray(window.selectedImages) && !window.selectedImages.includes(normalizedImage)) {
                    window.selectedImages.unshift(normalizedImage);
                }
                imageUrlsJsonInput.value = JSON.stringify(current);
            }

            if (imageUrlInput) {
                imageUrlInput.value = normalizedImage;
            }

            if (typeof window.refreshJournalImages === 'function') {
                window.refreshJournalImages();
            }
        } catch (error) {
            console.error(error);
            alert(error.message || msg('imageError', 'Failed to generate image. Please try again.'));
        } finally {
            loadingIndicator.style.display = 'none';
            generateButton.disabled = false;
        }
    });
});

async function generateImageFromJournalEntry(text, style, visualMemoryAnswer, contextImages) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            journalText: text,
            style,
            visualMemoryAnswer,
            contextImages
        })
    });

    if (response.ok) {
        const jsonResponse = await response.json();
        return jsonResponse.base64Image;
    }

    const raw = await response.text();
    console.error('Error generating image:', raw);
    try {
        const json = JSON.parse(raw);
        throw new Error(json.error || 'Image generation failed');
    } catch (error) {
        if (error instanceof SyntaxError) {
            throw new Error(raw || 'Image generation failed');
        }
        throw error;
    }
}
