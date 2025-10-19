

from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch
import numpy as np
import faiss
import os
import json
import matplotlib.pyplot as plt




device = "cuda" if torch.cuda.is_available() else "cpu"
model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32").to(device)
processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

image_folder = "images/"
index_file = "faiss_index.bin"
paths_file = "image_paths.json"




if os.path.exists(index_file) and os.path.exists(paths_file):

    index = faiss.read_index(index_file)
    print(f"Loaded FAISS index with {index.ntotal} vectors ")


    with open(paths_file, "r") as f:
        image_paths = json.load(f)
else:



    print("Index not found, building index from images...")
    embeddings = []
    image_paths = []

    for img_file in os.listdir(image_folder):
        img_path = os.path.join(image_folder, img_file)
        img = Image.open(img_path).convert("RGB")

        inputs = processor(images=img, return_tensors="pt").to(device)
        with torch.no_grad():
            emb = model.get_image_features(**inputs)

        embeddings.append(emb[0].cpu().numpy())
        image_paths.append(img_path)

    embeddings = np.stack(embeddings).astype("float32")


    dimension = embeddings.shape[1]
    index = faiss.IndexFlatL2(dimension)
    index.add(embeddings)
    print(f"Indexed {index.ntotal} images ")


    faiss.write_index(index, index_file)
    with open(paths_file, "w") as f:
        json.dump(image_paths, f)
    print("FAISS index and paths saved to disk ")




while True:
    query_text = input("\nEnter search text (or 'exit' to quit): ")
    if query_text.lower() == "exit":
        break

    inputs = processor(text=[query_text], return_tensors="pt", padding=True).to(device)
    with torch.no_grad():
        query_embedding = model.get_text_features(**inputs).cpu().numpy().astype("float32")

    k = min(1, len(image_paths))
    distances, indices = index.search(query_embedding, k=k)

    print("\nTop matches:")
    for rank, idx in enumerate(indices[0]):
        print(f"{rank+1}: {image_paths[idx]} (Distance: {distances[0][rank]:.4f})")


    for idx in indices[0]:
        img = Image.open(image_paths[idx])
        plt.imshow(img)
        plt.axis("off")
        plt.show()
