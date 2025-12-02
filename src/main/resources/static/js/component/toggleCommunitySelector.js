function toggleCommunitySelector() {
    // Get the visibility dropdown and community selector
    const visibilitySelect = document.getElementById('visibility');
    const communitySelector = document.getElementById('community-selector');

    // Check the selected value
    if (visibilitySelect.value === 'COMMUNITY') {
        communitySelector.style.display = 'block'; // Show the community selector
    } else {
        communitySelector.style.display = 'none'; // Hide the community selector
    }
}