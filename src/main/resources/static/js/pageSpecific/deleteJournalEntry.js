document.getElementById('deleteButton').addEventListener('click', function() {
    const i18n = window.journallyI18n || {};
    // Show a confirmation dialog
    if (confirm(i18n.deleteConfirm || 'Are you sure you want to delete this journal entry? This action cannot be undone.')) {
        // If confirmed, submit the hidden delete form
        document.getElementById('deleteForm').submit();
    }
});
