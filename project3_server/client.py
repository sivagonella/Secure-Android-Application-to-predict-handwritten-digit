import requests, cv2
  
# defining the api-endpoint 
API_ENDPOINT = "http://192.168.0.127:9001/upload"
  
image = cv2.imread("exam.jpg", cv2.IMREAD_GRAYSCALE)
print(image.shape)
# data to be sent to api
data = {'image': open('exam.jpg', 'rb')}
  
# sending post request and saving response as response object
r = requests.post(url = API_ENDPOINT, files = data)
  
# extracting response text 
# pastebin_url = r.text
# print("The pastebin URL is:%s"%pastebin_url)
print(r.content)