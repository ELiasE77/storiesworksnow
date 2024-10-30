document.getElementById('deleteButton').addEventListener('click', function() {
    // Show a confirmation dialog
    if (confirm('Are you sure you want to delete this journal entry? This action cannot be undone.')) {
        // If confirmed, submit the hidden delete form
        document.getElementById('deleteForm').submit();
    }
});