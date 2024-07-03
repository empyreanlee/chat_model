from django.shortcuts import render
from django.http import HttpResponse, JsonResponse
import json, socket, threading
from django.views.decorators.csrf import csrf_exempt

java_sock = None
request_lock = threading.Condition()
id_lock = threading.Condition()
response_data = None
global_request_id = 0

def getAndIncrement():
    global global_request_id, id_lock
    with id_lock:
        global_request_id +=1
        return global_request_id

def index(request):
    if request.method == 'GET':
        return render(request, 'index.html')
    else:
        return HttpResponse ('') 
    
@csrf_exempt
def arranca(request):
    global java_sock, request_lock, response_data
    if request.method == 'GET':
        request_id = getAndIncrement()
        data = json.loads(request.body)
        data['request_id'] = request_id 
        if java_sock: 
            java_sock.sendall((json.dumps(data) + "\n").encode())
            print(data, java_sock)      
        else:
            return HttpResponse('Java socket is not connected', status=500)        
        
        with request_lock:
            while response_data is None or response_data.get("request_id") != request_id:
                request_lock.wait()
        print(response_data)
        
        return JsonResponse(response_data)
    else:
        return HttpResponse('')


def server():
   global java_sock, request_lock, response_data          
   port = 1000            

   with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
       server_socket.bind(("0.0.0.0", port))
       server_socket.listen()
       print("Python server listening on port", port)
       
       while True:
           java_sock, addr = server_socket.accept()
           print("Connected to Java client at",  addr)
           while True:
               response_from_java = java_sock.recv(4096)
               print(response_from_java)
               with request_lock:
                   response_data = json.loads(response_from_java.decode())
                   request_lock.notifyAll()
    
def start_server_thread():
    server_thread = threading.Thread(target=server)
    server_thread.daemon = True
    server_thread.start()        