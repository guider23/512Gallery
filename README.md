# Content-Based Image Retrieval System

A small web app that finds similar images from text. It uses CLIP under the hood — basically, it looks at images and words like it actually knows what they mean.

## What it does

- Upload your images (so the model has something to pretend to understand).  
- Search using text (type "a cat on a chair" and pray it doesn’t give you a toaster).  
- Uses OpenAI’s CLIP model because it’s smart enough and free enough.  
- Stores image features in Pinecone so you don’t cry searching through arrays.  
- Images are hosted on ImgBB because who’s paying for S3, right?

## How it runs

It’s built with **Flask**, some **PyTorch**, and **CLIP** — basically Python, AI, and a lot of pretending things will work on the first try.  
The backend builds a FAISS index from your images, and the frontend just shows you what it *thinks* is similar.

---

Try it on Hugging Face Spaces (the “cosine” version is hosted there).  

🙂 [**512 Gallery on Hugging Face**](https://huggingface.co/spaces/kaniskaZoro/512Gallery)



<img width="1410" height="244" alt="image" src="https://github.com/user-attachments/assets/281cbdb7-88c7-4213-b129-2f8c0dd20d79" />

