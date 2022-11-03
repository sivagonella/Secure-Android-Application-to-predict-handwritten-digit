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

        imagefile.save(filename)
        print(imagefile)

        image = cv2.imread(filename, cv2.IMREAD_GRAYSCALE)
        image = cv2.resize(image, (28,28))
        image = np.array(image)
        image = image.reshape(1,28,28,1)

        model = load_model('mnist.h5')

        res = model.predict([image])[0]
        number = np.argmax(res)
        print(type(number))

        path = str(number) + '/'

        if(os.path.exists(path)):
            os.rename(filename, path+filename)
            
        else:
            os.mkdir(path)
            os.rename(filename, path + filename)

        return jsonify({
            "message": "Image Uploaded Successfully ",
        })

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True, port=9000)
