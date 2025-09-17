// Show community dropdown when "Community" visibility is selected
document.getElementById('visibility').addEventListener('change', function() {
    if (this.value === 'COMMUNITY') {
        document.getElementById('community-selector').style.display = 'block';
    } else {
        document.getElementById('community-selector').style.display = 'none';
    }
});