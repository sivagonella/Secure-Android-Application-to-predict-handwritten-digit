from flask import Flask, request, jsonify
import werkzeug, os
app = Flask(__name__)


@app.route('/')
def index():
    return 'App with Python Flask!'
    
@app.route('/upload', methods=["POST"])
def upload():
    if request.method == "POST" :
        imagefile = request.files['image']
        imageCategory = request.form['category']

        filename = werkzeug.utils.secure_filename(imagefile.filename)
        print("\nReceived image File name : " + imagefile.filename)

        path = imageCategory + '/'

        if(os.path.exists(path)):
            imagefile.save(path + filename)
            
        else:
            os.mkdir(path)
            imagefile.save(path + filename)

        return jsonify({
            "message": "Image Uploaded Successfully ",
        })

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True, port=9000)
