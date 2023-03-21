from flask import Flask, request, jsonify
import werkzeug, os, cv2, requests
from keras.models import load_model
import numpy as np
from datetime import datetime
import io
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from cryptography.hazmat.primitives.asymmetric import padding



app = Flask(__name__)

aesKey = b"0"
# Generating a private key
private_key = rsa.generate_private_key(public_exponent=65537,key_size=2048)

# Serializing the private key into a PEM encoded string
private_key_pem = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.PKCS8,
    encryption_algorithm=serialization.NoEncryption()
)

# Serializing the public key into a PEM encoded string
public_key_pem = private_key.public_key().public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Save the private key to disk
with open("private_key.pem", "wb") as f:
    f.write(private_key_pem)

# Save the public key to disk
with open("public_key.pem", "wb") as f:
    f.write(public_key_pem)



@app.route('/')
def index():
    return jsonify(publicKey=public_key_pem.decode("utf-8"))

@app.route("/aesKey", methods=["POST"])
def share_aes_key():
    encrypted_aes_key = request.data

    # Decrypting AES key with RSA private key
    with open("private_key.pem", "rb") as key_file:
        private_key_for_aes = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )
    # rsa_cipher = PKCS1_OAEP.new(private_key)
    # aes_key = rsa_cipher.decrypt(encrypted_aes_key)

    aes_key = private_key_for_aes.decrypt(
        encrypted_aes_key,
        padding.PKCS1v15()
    )

    # Saving the decrypted AES key for later use
    global aesKey 
    aesKey = aes_key

    # print("#" * 100)
    # print(aesKey)
    # print("#" * 100)

    return "AES key received and decrypted", 200
    
@app.route('/upload', methods=["POST"])
def upload():
    date = datetime.now()
    if request.method == "POST" :
        imagefile = request.files['image']
        # imageCategory = request.form['category']

        filename = werkzeug.utils.secure_filename(imagefile.filename)
        print("\nReceived image File name : " + imagefile.filename)

        imagefile.save(filename)


        # Read the encrypted file into memory
        with open(filename, "rb") as f:
            encrypted_file = f.read()

        # Create a secret key

        # print("*" * 100)
        # print(aesKey)
        # print("*" * 100)

        # Set up the decryption cipher
        cipher = Cipher(algorithms.AES(aesKey), modes.ECB(), backend=default_backend())
        decryptor = cipher.decryptor()

        # Decrypt the encrypted file
        decrypted_file = decryptor.update(encrypted_file) + decryptor.finalize()

        # Write the decrypted file to disk
        decrypted_file_name = "decfile" + str(date) + ".png"
        with open(decrypted_file_name, "wb") as f:
            f.write(decrypted_file)



        # print(imagefile)
        image = cv2.imread(decrypted_file_name, cv2.IMREAD_GRAYSCALE)
        image = cv2.resize(image, (28,28))
        imagePart1 = image[:14,:14]
        imagePart2 = image[:14,14:]
        imagePart3 = image[14:,:14]
        imagePart4 = image[14:,14:]
        
        cv2.imwrite('imagePart1.png', imagePart1)
        cv2.imwrite('imagePart2.png', imagePart2)
        cv2.imwrite('imagePart3.png', imagePart3)
        cv2.imwrite('imagePart4.png', imagePart4)

        # model = load_model('deepLearningModel')

        # res = model.predict([image])[0]
        # number = np.argmax(res)

        data1 = {'image': open('imagePart1.png', 'rb')}
        data2 = {'image': open('imagePart2.png', 'rb')}
        data3 = {'image': open('imagePart3.png', 'rb')}
        data4 = {'image': open('imagePart4.png', 'rb')}

        r1 = requests.post("http://192.168.0.125:9001/upload", files = data1)
        r2 = requests.post("http://192.168.0.125:9002/upload", files = data2)
        r3 = requests.post("http://192.168.0.125:9003/upload", files = data3)
        r4 = requests.post("http://192.168.0.125:9004/upload", files = data4)

        number1 = r1.json()['number']
        print("Number predicted by first model is " + str(number1))
        number2 = r2.json()['number']
        print("Number predicted by second model is " + str(number2))
        number3 = r3.json()['number']
        print("Number predicted by third model is " + str(number3))
        number4 = r4.json()['number']
        print("Number predicted by fourth model is " + str(number4))

        list = [0] * 10
        list[number1] += 1
        list[number2] += 1
        list[number3] += 1
        list[number4] += 1

        number = list.index(max(list))

        os.remove("imagePart1.png")
        os.remove("imagePart2.png")
        os.remove("imagePart3.png")
        os.remove("imagePart4.png")

        path = str(number) + '/'

        if(os.path.exists(path)):
            os.rename(decrypted_file_name, path+decrypted_file_name)
            
        else:
            os.mkdir(path)
            os.rename(decrypted_file_name, path + decrypted_file_name)

        return jsonify({
            "message": "Image Uploaded Successfully ",
            "number": int(number),
        })

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True, port=9000)
