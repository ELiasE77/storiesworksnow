(() => {
    const listSelector = '#journal-entry-list';
    const filterSelector = '#month-year-filter';
    const headingSelector = '#h2';

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

    const renderLoading = (list) => {
        clearList(list);
        const li = document.createElement('li');
        li.className = 'journal-entry skeleton';

        const storyItem = document.createElement('div');
        storyItem.className = 'story-item';

        const media = document.createElement('div');
        media.className = 'journal-media';
        const placeholder = document.createElement('div');
        placeholder.className = 'image-placeholder skeleton-block';
        media.appendChild(placeholder);
        storyItem.appendChild(media);

        const content = document.createElement('div');
        content.className = 'story-content';
        ['skeleton-title', 'skeleton-subtitle', '', 'short', 'skeleton-timestamp'].forEach((klass, index) => {
            const line = document.createElement('div');
            line.className = 'skeleton-line' + (klass ? ' ' + klass : '');
            if (index === 3) {
                line.classList.add('short');
            }
            content.appendChild(line);
        });
        storyItem.appendChild(content);

        li.appendChild(storyItem);
        list.appendChild(li);
    };

    const fetchEntries = async (month, year) => {
        const params = new URLSearchParams();
        if (month) params.set('month', month);
        if (year) params.set('year', year);

        const query = params.toString();
        const response = await fetch(query ? `/api/journal/entries?${query}` : '/api/journal/entries');
        if (!response.ok) {
            throw new Error('Failed to fetch entries');
        }
        return response.json();
    };

    const getSelectedMonthYear = () => {
        const select = document.querySelector(filterSelector);
        if (!select) return {};
        const selectedOption = select.options[select.selectedIndex];
        const month = selectedOption?.dataset.month;
        const year = selectedOption?.dataset.year;
        return {
            month: month ? parseInt(month, 10) : undefined,
            year: year ? parseInt(year, 10) : undefined,
            label: selectedOption?.textContent || ''
        };
    };

    const updateUrl = (month, year) => {
        const params = new URLSearchParams(window.location.search);
        if (month) {
            params.set('month', month);
        } else {
            params.delete('month');
        }
        if (year) {
            params.set('year', year);
        } else {
            params.delete('year');
        }
        const newUrl = `${window.location.pathname}?${params.toString()}`.replace(/\?$/, '');
        window.history.replaceState({}, '', newUrl);    };

    const updateHeading = (label) => {
        const heading = document.querySelector(headingSelector);
        if (!heading) return;
        heading.textContent = label ? `Entries for ${label}` : 'Recent Journal Entries';
    };

    document.addEventListener('DOMContentLoaded', async () => {
        const list = document.querySelector(listSelector);
        if (!list) {
            return;
        }

        const { month, year, label } = getSelectedMonthYear();
        updateHeading(label);

        try {
            renderLoading(list);
            const entries = await fetchEntries(month, year);
            renderEntries(list, entries);
        } catch (error) {
            renderError(list);
            console.error('Could not load journal entries', error);
        }

        const filter = document.querySelector(filterSelector);
        if (filter) {
            filter.addEventListener('change', async () => {
                const { month: m, year: y, label: currentLabel } = getSelectedMonthYear();
                updateHeading(currentLabel);
                try {
                    renderLoading(list);
                    const entries = await fetchEntries(m, y);
                    renderEntries(list, entries);
                    updateUrl(m, y);
                } catch (error) {
                    renderError(list);
                    console.error('Could not load journal entries', error);
                }
            });
        }
    });
})();