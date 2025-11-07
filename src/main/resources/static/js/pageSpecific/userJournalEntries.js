(() => {
    const listSelector = '#journal-entry-list';

    const clearList = (list) => {
        while (list.firstChild) {
            list.removeChild(list.firstChild);
        }
    };

    const renderNoEntries = (list) => {
        clearList(list);
        const li = document.createElement('li');
        li.className = 'no-entries-message';
        li.textContent = 'You have not written any entries yet!';
        list.appendChild(li);
    };

    const renderError = (list) => {
        clearList(list);
        const li = document.createElement('li');
        li.className = 'error-message';
        li.textContent = 'We could not load your journal entries. Please try again later.';
        list.appendChild(li);
    };

    const createMediaContainer = (entry) => {
        if (!entry.hasImage) {
            return null;
        }
        const mediaContainer = document.createElement('div');
        mediaContainer.className = 'journal-media';
        mediaContainer.setAttribute('data-entry-id', entry.id);

        const placeholder = document.createElement('div');
        placeholder.className = 'image-placeholder';
        placeholder.textContent = 'Loading image…';

        mediaContainer.appendChild(placeholder);
        return mediaContainer;
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) {
            return '';
        }
        const date = new Date(timestamp);
        if (Number.isNaN(date.getTime())) {
            return `Posted on: ${timestamp}`;
        }
        const formatted = date.toLocaleString([], {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
        return `Posted on: ${formatted}`;
    };

    const createEntryElement = (entry) => {
        const li = document.createElement('li');
        li.className = 'journal-entry';
        li.setAttribute('data-entry-id', entry.id);
        li.setAttribute('data-has-image', String(entry.hasImage));

        const storyItem = document.createElement('div');
        storyItem.className = 'story-item';

        const mediaContainer = createMediaContainer(entry);
        if (mediaContainer) {
            storyItem.appendChild(mediaContainer);
        }

        const content = document.createElement('div');
        content.className = 'story-content';

        const title = document.createElement('h3');
        title.className = 'journal-title';
        title.textContent = entry.title;
        content.appendChild(title);

        const username = document.createElement('h4');
        username.className = 'journal-username';
        username.textContent = entry.username;
        content.appendChild(username);

        const text = document.createElement('p');
        text.className = 'journal-text';
        text.textContent = entry.content;
        content.appendChild(text);

        const timestamp = document.createElement('small');
        timestamp.className = 'journal-timestamp';
        timestamp.textContent = formatTimestamp(entry.timestamp);
        content.appendChild(timestamp);

        storyItem.appendChild(content);

        const editLink = document.createElement('a');
        editLink.href = `/journal/edit?id=${entry.id}`;
        editLink.textContent = 'Edit';
        storyItem.appendChild(editLink);

        li.appendChild(storyItem);

        return li;
    };

    const renderEntries = (list, entries) => {
        if (!entries.length) {
            renderNoEntries(list);
            return;
        }

        clearList(list);
        const fragment = document.createDocumentFragment();
        entries.forEach((entry) => {
            fragment.appendChild(createEntryElement(entry));
        });
        list.appendChild(fragment);

        const entriesWithImages = list.querySelectorAll('.journal-entry[data-has-image="true"]');
        if (window.JournalImages && typeof window.JournalImages.loadForEntries === 'function') {
            window.JournalImages.loadForEntries(entriesWithImages);
        }
    };

    const fetchEntries = async () => {
        const response = await fetch('/api/journal/entries');
        if (!response.ok) {
            throw new Error('Failed to fetch entries');
        }
        return response.json();
    };

    document.addEventListener('DOMContentLoaded', async () => {
        const list = document.querySelector(listSelector);
        if (!list) {
            return;
        }

        try {
            const entries = await fetchEntries();
            renderEntries(list, entries);
        } catch (error) {
            renderError(list);
            console.error('Could not load journal entries', error);
        }
    });
})();