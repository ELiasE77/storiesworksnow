(function () {
    /**
     * Loads the image for a specific journal entry element.
     * @param {HTMLElement} entryElement - The journal entry element.
     */
    const loadImageForEntry = async (entryElement) => {
        const entryId = entryElement.getAttribute('data-entry-id');
        const mediaContainer = entryElement.querySelector('.journal-media');

        if (!entryId || !mediaContainer) {
            return;
        }

        const placeholder = mediaContainer.querySelector('.image-placeholder');

        try {
            const response = await fetch(`/api/journal/${entryId}/image`);
            if (!response.ok) {
                if (placeholder) placeholder.textContent = 'Unable to load image';
                return;
            }

            const payload = await response.json();
            if (!payload.imageUrl) {
                mediaContainer.remove();
                return;
            }

            const img = document.createElement('img');
            img.classList.add('journal-image');
            img.alt = 'Journal entry image';
            img.src = `data:image/png;base64,${payload.imageUrl}`;

            mediaContainer.innerHTML = '';
            mediaContainer.appendChild(img);
        } catch (error) {
            if (placeholder) placeholder.textContent = 'Image failed to load';
        }
    };

    /**
     * Loads images for multiple entries sequentially.
     * @param {HTMLElement[]} entryElements - Array of journal entry elements.
     */
    const loadSequentially = async (entryElements) => {
        if (!entryElements || !entryElements.length) {
            return;
        }

        for (const entryElement of entryElements) {
            await loadImageForEntry(entryElement);
        }
    };

    /**
     * Public API to manually trigger image loading for journal entries.
     */
    window.JournalImages = {
        loadForEntries: (entryElements) => {
            const elements = Array.from(entryElements || []);
            return loadSequentially(elements);
        }
    };

    /**
     * Automatically trigger loading when DOM is ready.
     */
    document.addEventListener('DOMContentLoaded', () => {
        const entryElements = Array.from(
            document.querySelectorAll('.journal-entry[data-has-image="true"]')
        );
        loadSequentially(entryElements);
    });
})();
