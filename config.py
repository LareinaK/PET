
import os
class Config(object):
  BASEDIR = os.path.abspath(os.path.dirname(__file__))
  DATABASE = BASEDIR+'/var/pi.db'
  SECRET_KEY = os.urandom(24)
  USERNAME = 'admin'
  PASSWORD = 'default'
  DEBUG = True
  HOST = '0.0.0.0'
  PORT = 9001
class Development(Config):
  pass
class Production(Config):
  DEBUG = False
  HOST = '0.0.0.0'
  PORT = 8080
