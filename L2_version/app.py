"""
CBIR Web App - Using EXACT logic from full.py
- L2 Distance (IndexFlatL2)
- No normalization
- Lower distance = better match
"""

from flask import Flask, render_template, request, jsonify
from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch
import numpy as np
import faiss
import os
import json
from werkzeug.utils import secure_filename
import base64
from io import BytesIO

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'images'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}


device = "cuda" if torch.cuda.is_available() else "cpu"
model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32").to(device)
processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

index_file = "faiss_index.bin"
paths_file = "image_paths.json"

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_image_embedding(img_path):
    """Generate embedding - EXACT same as full.py (no normalization)"""
    img = Image.open(img_path).convert("RGB")
    inputs = processor(images=img, return_tensors="pt").to(device)
    with torch.no_grad():
        emb = model.get_image_features(**inputs)

    return emb[0].cpu().numpy().astype("float32")

def rebuild_index():
    """Rebuild FAISS index - EXACT same as full.py"""
    embeddings = []
    image_paths = []
    
    if not os.path.exists(app.config['UPLOAD_FOLDER']):
        os.makedirs(app.config['UPLOAD_FOLDER'])
        return None, []
    
    print(f"\nRebuilding index from {app.config['UPLOAD_FOLDER']}...")
    
    for img_file in os.listdir(app.config['UPLOAD_FOLDER']):
        if allowed_file(img_file):
            img_path = os.path.join(app.config['UPLOAD_FOLDER'], img_file)
            try:
                emb = get_image_embedding(img_path)
                embeddings.append(emb)
                image_paths.append(img_path)
                print(f"  ✓ Indexed: {img_file}")
            except Exception as e:
                print(f"  ✗ Error processing {img_path}: {e}")
                continue
    
    if not embeddings:
        return None, []
    
    embeddings = np.stack(embeddings).astype("float32")
    

    dimension = embeddings.shape[1]
    index = faiss.IndexFlatL2(dimension)
    index.add(embeddings)
    

    faiss.write_index(index, index_file)
    with open(paths_file, "w") as f:
        json.dump(image_paths, f)
    
    print(f" Index built with {len(image_paths)} images (L2 distance)\n")
    
    return index, image_paths

def load_or_create_index():
    """Load existing index or create new one"""
    if os.path.exists(index_file) and os.path.exists(paths_file):
        index = faiss.read_index(index_file)
        with open(paths_file, "r") as f:
            image_paths = json.load(f)
        print(f"Loaded existing index with {len(image_paths)} images")
        return index, image_paths
    else:
        return rebuild_index()


index, image_paths = load_or_create_index()

@app.route('/')
def home():
    return render_template('index.html')

@app.route('/upload', methods=['POST'])
def upload_image():
    global index, image_paths
    
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400
    
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        

        base, ext = os.path.splitext(filename)
        counter = 1
        while os.path.exists(os.path.join(app.config['UPLOAD_FOLDER'], filename)):
            filename = f"{base}_{counter}{ext}"
            counter += 1
        
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        

        index, image_paths = rebuild_index()
        
        return jsonify({
            'success': True,
            'message': f'Image uploaded and indexed successfully',
            'total_images': len(image_paths)
        })
    
    return jsonify({'error': 'Invalid file type'}), 400

@app.route('/search', methods=['POST'])
def search():
    data = request.json
    query_text = data.get('query', '')
    
    if not query_text:
        return jsonify({'error': 'No query provided'}), 400
    
    if index is None or len(image_paths) == 0:
        return jsonify({'error': 'No images indexed yet'}), 400
    
    print(f"\n Searching for: '{query_text}'")
    print(f" Total images in index: {len(image_paths)}")
    

    inputs = processor(text=[query_text], return_tensors="pt", padding=True).to(device)
    with torch.no_grad():
        query_embedding = model.get_text_features(**inputs).cpu().numpy().astype("float32")
    

    

    k = min(1, len(image_paths))
    distances, indices = index.search(query_embedding, k=k)
    
    print(f" Best match: {image_paths[indices[0][0]]} (distance: {distances[0][0]:.4f})")
    
    results = []
    for rank, idx in enumerate(indices[0]):
        img_path = image_paths[idx]
        

        with Image.open(img_path) as img:

            img.thumbnail((800, 800), Image.Resampling.LANCZOS)
            buffered = BytesIO()
            img.save(buffered, format="PNG")
            img_str = base64.b64encode(buffered.getvalue()).decode()
        
        results.append({
            'rank': rank + 1,
            'path': img_path,
            'filename': os.path.basename(img_path),
            'distance': float(distances[0][rank]),
            'score': f"{1 / (1 + distances[0][rank]):.4f}",
            'image_data': f'data:image/png;base64,{img_str}'
        })
    
    return jsonify({'results': results})

@app.route('/stats')
def stats():
    return jsonify({
        'total_images': len(image_paths) if image_paths else 0,
        'index_exists': index is not None,
        'method': 'L2 Distance (IndexFlatL2)'
    })

if __name__ == '__main__':
    print("\n" + "="*60)
    print("CBIR System - Using EXACT full.py Logic")
    print("Method: L2 Distance (no normalization)")
    print("="*60 + "\n")
    app.run(debug=True, host='0.0.0.0', port=5001)
