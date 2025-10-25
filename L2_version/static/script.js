const uploadArea = document.getElementById('upload-area');
const fileInput = document.getElementById('file-input');
const uploadStatus = document.getElementById('upload-status');
const searchInput = document.getElementById('search-input');
const searchBtn = document.getElementById('search-btn');
const searchStatus = document.getElementById('search-status');
const resultsSection = document.getElementById('results-section');
const resultsContainer = document.getElementById('results-container');
const imageCount = document.getElementById('image-count');

updateStats();

uploadArea.addEventListener('click', () => {
    fileInput.click();
});

fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
        uploadFile(e.target.files[0]);
    }
});

uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.classList.add('dragover');
});

uploadArea.addEventListener('dragleave', () => {
    uploadArea.classList.remove('dragover');
});

uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
    
    if (e.dataTransfer.files.length > 0) {
        uploadFile(e.dataTransfer.files[0]);
    }
});

searchBtn.addEventListener('click', () => {
    performSearch();
});

searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        performSearch();
    }
});

async function uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    showStatus(uploadStatus, 'info', 'Uploading and indexing image...');
    
    try {
        // Because the network *never* fails, right?
        const response = await fetch('/upload', {
            method: 'POST',
            body: formData
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showStatus(uploadStatus, 'success', `${data.message}. Total: ${data.total_images} images.`);
            updateStats();
            fileInput.value = '';
        } else {
            showStatus(uploadStatus, 'error', data.error || 'Upload failed');
        }
    } catch (error) {
        showStatus(uploadStatus, 'error', 'Network error. Please try again.');
    }
}

async function performSearch() {
    const query = searchInput.value.trim();
    
    if (!query) {
        // Because searching with an empty box is apparently a thing.
        showStatus(searchStatus, 'error', 'Please enter a search query');
        return;
    }
    
    searchBtn.disabled = true;
    searchBtn.innerHTML = '<span class="spinner"></span> Searching...';
    showStatus(searchStatus, 'info', 'Searching...');
    resultsSection.style.display = 'none';
    
    try {
        const response = await fetch('/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ query: query })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Look at that it actually worked.
            displayResults(data.results);
            showStatus(searchStatus, 'success', `Found ${data.results.length} result(s)`);
        } else {
            showStatus(searchStatus, 'error', data.error || 'Search failed');
        }
    } catch (error) {
        showStatus(searchStatus, 'error', 'Network error. Please try again.');
    } finally {
        searchBtn.disabled = false;
        searchBtn.innerHTML = `
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
            </svg>
            Search
        `;
    }
}

function displayResults(results) {
    resultsContainer.innerHTML = '';
    
    if (results.length === 0) {
        // Ah yes, your query was *too* good. No matches.
        resultsContainer.innerHTML = '<p style="color: var(--text-secondary);">No results found</p>';
        resultsSection.style.display = 'block';
        return;
    }
    
    results.forEach(result => {
        const resultItem = document.createElement('div');
        resultItem.className = 'result-item';
        
        resultItem.innerHTML = `
            <img src="${result.image_data}" alt="${result.filename}" class="result-image">
            <div class="result-info">
                <div class="result-filename">${result.filename}</div>
                <div class="result-meta">
                    <span>
                        <div class="label">Rank</div>
                        <div>#${result.rank}</div>
                    </span>
                    <span>
                        <div class="label">L2 Distance</div>
                        <div>${result.distance.toFixed(4)}</div>
                    </span>
                    <span>
                        <div class="label">Score</div>
                        <div>${result.score}</div>
                    </span>
                </div>
            </div>
        `;
        
        resultsContainer.appendChild(resultItem);
    });
    
    resultsSection.style.display = 'block';
}

function showStatus(element, type, message) {
    element.className = `status-message show ${type}`;
    element.textContent = message;
    
    setTimeout(() => {
        element.classList.remove('show');
    }, 5000);
}

async function updateStats() {
    try {
        const response = await fetch('/stats');
        const data = await response.json();
        // Because we totally trust the backend to always return correct stats.
        imageCount.textContent = `${data.total_images} images indexed (${data.method})`;
    } catch (error) {
        // Shocking something went wrong.
        console.error('Failed to update stats:', error);
    }
}
