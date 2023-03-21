from flask import Flask, request, jsonify
import werkzeug, os, cv2
from keras.models import load_model
import numpy as np
app = Flask(__name__)


@app.route('/')
def index():
    return 'App with Python Flask!'
    
@app.route('/upload', methods=["POST"])
def upload():
    if request.method == "POST" :
        imagefile = request.files['image']
        # imageCategory = request.form['category']

        filename = werkzeug.utils.secure_filename(imagefile.filename)
        print("\nReceived image File name : " + imagefile.filename)

        image = cv2.imread(filename, cv2.IMREAD_GRAYSCALE)
        image = cv2.resize(image, (14,14))
        image = np.array(image)
        image = image.reshape(1,14,14,1)
        # image = 255 - image
        image = image/255.0

        model = load_model('deepLearningModelPart2')

        res = model.predict([image])
        print(res)
        number = np.argmax(res[0])

        return jsonify({
            "message": "Image Uploaded Successfully 2",
            "number": int(number),
        })

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True, port=9002)
