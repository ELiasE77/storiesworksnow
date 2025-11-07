(function () {
    document.addEventListener('DOMContentLoaded', () => {
        const entryElements = Array.from(
            document.querySelectorAll('.journal-entry[data-has-image="true"]')
        );

        if (!entryElements.length) {
            return;
        }

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
                    if (placeholder) {
                        placeholder.textContent = 'Unable to load image';
                    }
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
                if (placeholder) {
                    placeholder.textContent = 'Image failed to load';
                }
            }
        };

        (async () => {
            for (const entryElement of entryElements) {
                // Sequential loading keeps requests small and prioritises text rendering.
                await loadImageForEntry(entryElement);
            }
        })();
    });
})();