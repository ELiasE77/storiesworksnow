(() => {
    const listSelector = '#journal-entry-list';
    const selectSelector = '#journal-day-select';
    const prevButtonSelector = '#journal-prev';
    const nextButtonSelector = '#journal-next';
    const dayLabelSelector = '#journal-day-label';

    let dayKeys = [];
    let entriesByDay = new Map();
    let currentIndex = 0;

    const clearList = (list) => {
        while (list.firstChild) {
            list.removeChild(list.firstChild);
        }
    };

    const setControlsState = ({ disabled, prevDisabled, nextDisabled }) => {
        const select = document.querySelector(selectSelector);
        const prevButton = document.querySelector(prevButtonSelector);
        const nextButton = document.querySelector(nextButtonSelector);

        if (select) {
            select.disabled = Boolean(disabled);
        }
        if (prevButton) {
            prevButton.disabled = Boolean(disabled || prevDisabled);
        }
        if (nextButton) {
            nextButton.disabled = Boolean(disabled || nextDisabled);
        }
    };

    const getDayKey = (timestamp) => {
        if (!timestamp) {
            return '';
        }
        const date = new Date(timestamp);
        if (Number.isNaN(date.getTime())) {
            return String(timestamp).split('T')[0];
        }
        return date.toISOString().split('T')[0];
    };

    const formatDayLabel = (dayKey) => {
        if (!dayKey) {
            return '';
        }
        const date = new Date(`${dayKey}T00:00:00`);
        if (Number.isNaN(date.getTime())) {
            return dayKey;
        }
        return date.toLocaleDateString([], {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
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

    const renderNoEntries = (list) => {
        clearList(list);
        const li = document.createElement('li');
        li.className = 'no-entries-message';
        li.textContent = 'You have not written any entries yet!';
        list.appendChild(li);
        const dayLabel = document.querySelector(dayLabelSelector);
        if (dayLabel) {
            dayLabel.textContent = '';
        }
    };

    const renderError = (list) => {
        clearList(list);
        const li = document.createElement('li');
        li.className = 'error-message';
        li.textContent = 'We could not load your journal entries. Please try again later.';
        list.appendChild(li);
        const dayLabel = document.querySelector(dayLabelSelector);
        if (dayLabel) {
            dayLabel.textContent = '';
        }
        setControlsState({ disabled: true });
    };

    const createMediaContainer = (entry) => {
        if (!entry.hasImage) {
            return null;
        }
        const mediaContainer = document.createElement('div');
        mediaContainer.className = 'journal-media';

        const placeholder = document.createElement('div');
        placeholder.className = 'image-placeholder';
        placeholder.textContent = 'Loading image…';

        mediaContainer.appendChild(placeholder);
        return mediaContainer;
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
        editLink.className = 'edit-link';
        storyItem.appendChild(editLink);

        li.appendChild(storyItem);

        return li;
    };

    const renderEntries = (list, entries) => {
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

    const renderDayEntries = (list, dayKey) => {
        const entries = entriesByDay.get(dayKey) || [];
        const dayLabel = document.querySelector(dayLabelSelector);

        list.classList.add('is-transitioning');
        window.setTimeout(() => {
            renderEntries(list, entries.slice(0, 1));
            if (dayLabel) {
                const label = formatDayLabel(dayKey);
                dayLabel.textContent = entries.length
                    ? `Latest entry for ${label}`
                    : label;
            }
            list.classList.remove('is-transitioning');
        }, 150);
    };

    const updateNavigation = (list) => {
        if (!dayKeys.length) {
            renderNoEntries(list);
            setControlsState({ disabled: true });
            return;
        }

        currentIndex = Math.min(Math.max(currentIndex, 0), dayKeys.length - 1);
        const dayKey = dayKeys[currentIndex];
        renderDayEntries(list, dayKey);

        const select = document.querySelector(selectSelector);
        if (select) {
            select.value = dayKey;
        }
        setControlsState({
            disabled: false,
            prevDisabled: currentIndex === dayKeys.length - 1,
            nextDisabled: currentIndex === 0
        });
    };

    const buildDayIndex = (entries) => {
        entriesByDay = new Map();
        entries.forEach((entry) => {
            const key = getDayKey(entry.timestamp);
            if (!key) {
                return;
            }
            if (!entriesByDay.has(key)) {
                entriesByDay.set(key, []);
            }
            entriesByDay.get(key).push(entry);
        });

        entriesByDay.forEach((dayEntries) => {
            dayEntries.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        });

        dayKeys = Array.from(entriesByDay.keys()).sort((a, b) => {
            const dateA = new Date(`${a}T00:00:00`);
            const dateB = new Date(`${b}T00:00:00`);
            return dateB - dateA;
        });
        currentIndex = 0;
    };

    const populateSelect = () => {
        const select = document.querySelector(selectSelector);
        if (!select) {
            return;
        }
        select.innerHTML = '';
        dayKeys.forEach((dayKey) => {
            const option = document.createElement('option');
            option.value = dayKey;
            option.textContent = formatDayLabel(dayKey);
            select.appendChild(option);
        });
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
            if (!entries.length) {
                renderNoEntries(list);
                setControlsState({ disabled: true });
                return;
            }
            buildDayIndex(entries);
            populateSelect();
            updateNavigation(list);

            const select = document.querySelector(selectSelector);
            if (select) {
                select.addEventListener('change', (event) => {
                    const value = event.target.value;
                    const index = dayKeys.indexOf(value);
                    if (index !== -1) {
                        currentIndex = index;
                        updateNavigation(list);
                    }
                });
            }

            const prevButton = document.querySelector(prevButtonSelector);
            if (prevButton) {
                prevButton.addEventListener('click', () => {
                    if (currentIndex < dayKeys.length - 1) {
                        currentIndex += 1;
                        updateNavigation(list);
                    }
                });
            }

            const nextButton = document.querySelector(nextButtonSelector);
            if (nextButton) {
                nextButton.addEventListener('click', () => {
                    if (currentIndex > 0) {
                        currentIndex -= 1;
                        updateNavigation(list);
                    }
                });
            }
        } catch (error) {
            renderError(list);
            console.error('Could not load journal entries', error);
        }
    });
})();