import asyncio
import argparse
import json
import pathlib
import time
import logging
import os
import uuid
import os.path
from http import HTTPStatus
import math
from threading import Thread

import uvloop
import aiohttp

DATA = []

parser = argparse.ArgumentParser(description='Stress test micronaut')
parser.add_argument(
  '--duration',
  type=int,
  default=30,
  help='Duration of command in seconds'
)
parser.add_argument(
  '--rps',
  type=int,
  default=1,
  help='Amount of requests sent per second'
)
parser.add_argument(
  '--dataDir',
  type=pathlib.Path,
  default='/tmp/data'
)
parser.add_argument(
  '--host',
  default="http://localhost:8080",
)


async def wait_for_startup(host):
  while True:
    logging.info('Waiting app')
    try:
      async with aiohttp.ClientSession() as session:
        async with session.get(host) as r:
          if r.status:
            return
    except Exception as e:
      await asyncio.sleep(0.1)


async def set_up(host):
  logging.info('Setup start')
  headers = {'content-type': 'application/json'}
  email = f'{uuid.uuid4().hex}@example.com'
  data_signup = json.dumps({
    'email': email,
    'password': 'test'
  })
  data_login = json.dumps({
    'username': email,
    'password': 'test',
  })
  data_shorten = json.dumps({'url': 'https://google.com'})
  async with aiohttp.ClientSession(headers=headers) as session:
    async with session.post(f'{host}/users/signup', data=data_signup) as r:
      assert r.status == HTTPStatus.OK
    async with session.post(f'{host}/login', data=data_login) as r:
      assert r.status == HTTPStatus.OK
      json_body = await r.json()
      token = json_body['access_token']
      headers['Authorization'] = f'Bearer {token}'
  async with aiohttp.ClientSession(headers=headers) as session:
    async with session.post(f"{host}/urls/shorten", data=data_shorten) as r:
      assert r.status == HTTPStatus.OK
      alias = (await r.text()).split('/')[-1]
  logging.info('Setup end with token: %s alias: %s' % (token, alias))
  return alias


def request_type_generator():
  while True:
    for i in range(10):
      yield 'redirect'
    yield 'sign-up'
    yield 'sign-in'


def measure(func):
  async def wrapper(*args, **kwargs):
    start = time.time()
    success = True
    try:
      resp = await func(*args, **kwargs)
      assert resp.status != 500
    except Exception as e:  # all connection errors (server down) also go here
      success = False
      return
    msg = {
      'success': success,
      'time': time.time() - start
    }
    logging.info(msg)
    DATA.append(msg)
  return wrapper


@measure
async def redirect(host, alias):
  async with aiohttp.ClientSession() as session:
    async with session.get(f'{host}/r/{alias}') as response:
      return response


@measure
async def sign_up(host):
  data = json.dumps({
    'email': f'{uuid.uuid4().hex}@example.com',
    'password': 'test'
  })
  headers = {'content-type': 'application/json'}
  async with aiohttp.ClientSession(headers=headers) as session:
    async with session.post(f'{host}/users/signup', data=data) as response:
      return response


@measure
async def sign_in(host):
  data = json.dumps({
    'username': f'{uuid.uuid4().hex}@example.com',
    'password': 'test'
  })
  headers = {'content-type': 'application/json'}
  async with aiohttp.ClientSession(headers=headers) as session:
    async with session.post(f'{host}/login', data=data) as response:
      return response


async def make_request(host, type_, alias):
  if type_ == 'redirect':
    await redirect(host, alias)
  elif type_ == 'sign-up':
    await sign_up(host)
  elif type_ == 'sign-in':
    await sign_in(host)


async def thread_function(host, duration, rps):
  await wait_for_startup(host)
  alias = await set_up(host)

  start = time.time()
  delay_seconds = 1 / rps
  coroutines = []
  for type_ in request_type_generator():
    coroutines.append(
      asyncio.get_event_loop().create_task(make_request(host, type_, alias))
    )
    await asyncio.sleep(delay_seconds)
    if time.time() > start + duration:
      break

  await asyncio.gather(*coroutines)


def thread_generator(host, duration, rps):
  loop = asyncio.new_event_loop()
  return Thread(target=lambda: loop.run_until_complete(thread_function(host, duration, rps)))


if __name__ == '__main__':
  logging.getLogger().setLevel(logging.INFO)
  uvloop.install()

  args = parser.parse_args()
  data_dir = args.dataDir
  duration = args.duration
  host = args.host
  rps = args.rps

  n_threads = math.ceil(args.rps / 50)
  threads = []
  for i in range(n_threads):
    thread = thread_generator(host, duration, math.ceil(rps / n_threads))
    thread.start()
    threads.append(thread)

  for t in threads:
    t.join()

  try:
    os.mkdir(data_dir)
  except FileExistsError:
    pass
  out_path = os.path.join(data_dir, f'{duration}-{rps}')
  with open(out_path, 'w+') as file:
    json.dump(DATA, file)
  os.chmod(out_path, 0o777)
  logging.info('Stats saved to %s', out_path)
