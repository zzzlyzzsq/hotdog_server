#!/usr/bin/python
# -*- coding: UTF-8 -*-
#change
import os, sys

import tensorflow as tf

import re
import time
import socket

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

fileName=sys.argv[1]

# change this as you see fit
#image_path = sys.argv[1]
actual_count = 0;
total = 0;


start_time1 = time.time()
with tf.gfile.FastGFile("./retrained_graph.pb", 'rb') as f:
	graph_def = tf.GraphDef()
	graph_def.ParseFromString(f.read())
	tf.import_graph_def(graph_def, name='')
end_time1 = time.time()

# skt = socket.socket()
# host = '10.0.0.24'
# port = 3333
# skt.bind((host,port))


#for fileName in os.listdir('/Users/Junjie/Desktop/Hotdog-Classification-master/edge/'):
total = total + 1
# Read in the image_data
filePath = './edge/' + fileName
image_data = tf.gfile.FastGFile(filePath, 'rb').read()

# Loads label file, strips off carriage return
label_lines = [line.rstrip() for line in tf.gfile.GFile("./retrained_labels.txt")]

# Unpersists graph from file


with tf.Session() as sess:
	# Feed the image_data as input to the graph and get first prediction
	softmax_tensor = sess.graph.get_tensor_by_name('final_result:0')
	try:
		predictions = sess.run(softmax_tensor, \
		 {'DecodeJpeg/contents:0': image_data})
		# Sort to show labels of first prediction in order of confidence
		top_k = predictions[0].argsort()[-len(predictions[0]):][::-1]
		for node_id in top_k:
			human_string = label_lines[node_id]
			score = predictions[0][node_id]
			#rint('%s (score = %.5f)' % (human_string, score))
		print(label_lines[top_k[0]])
		result = label_lines[top_k[0]]
		m = re.search('no', fileName)
		if (result == 'random' and m) or (result == 'hotdogs' and not m):
			actual_count = actual_count + 1


	except tf.errors.InvalidArgumentError:
		print(fileName)
		#continue

	#set up socket server

# skt.listen(5)
# conn,addr = skt.accept()
# print ("Got connection from",addr)
# tmp = fileName + '\n'
# msg = conn.send(tmp.encode('utf-8'))

	
		
#print('Failure rate is ', (1- actual_count/total)*100 ,'%')

