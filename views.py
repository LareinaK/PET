
from flask import Flask, request, session, g, redirect, url_for, abort, render_template, flash
from contextlib import closing
from pi_car import app
import re
import RPi.GPIO as GPIO
import  time 
import signal
import atexit
atexit.register(GPIO.cleanup) 
@app.route('/')
def show_index():
	return render_template('home.html')


IN1 = 18
IN2 = 23
IN3 = 24
IN4 = 25

@app.route('/cmd',methods=['GET','POST'])
def ctrl_id():
	if request.method == 'POST':
		id=request.form['id']
		GPIO.setmode(GPIO.BCM)
		GPIO.setwarnings(False)
		GPIO.setup(IN1,GPIO.OUT)
		GPIO.setup(IN2,GPIO.OUT)
		GPIO.setup(IN3,GPIO.OUT)
		GPIO.setup(IN4,GPIO.OUT)
		print id
		if id == 't_left':
			t_left()
			return "left"
		elif id == 't_right':
			t_right()
			return "right"
		elif id == 't_up':
			t_up()
			return "up"
		elif id == 't_down':
			t_down()
			return "down"
		elif id == 't_stop':
			t_stop()
			return "stop"
		
	return redirect(url_for('show_index'))

def t_stop():
	GPIO.output(IN1, False)
	GPIO.output(IN2, False)
	GPIO.output(IN3, False)
	GPIO.output(IN4, False)

def t_up():
	GPIO.output(IN1, True)
	GPIO.output(IN2, False)
	GPIO.output(IN3, True)
	GPIO.output(IN4, False)

def t_down():
	GPIO.output(IN1, False)
	GPIO.output(IN2, True)
	GPIO.output(IN3, False)
	GPIO.output(IN4, True)

def t_left():
	GPIO.output(IN1, False)
	GPIO.output(IN2, True)
	GPIO.output(IN3, True)
	GPIO.output(IN4, False)

def t_right():
	GPIO.output(IN1, True)
	GPIO.output(IN2, False)
	GPIO.output(IN3, False)
	GPIO.output(IN4, True)
