import uvicorn
from hue_ai.app import create_app
app = create_app()
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001, reload=False, workers=1)