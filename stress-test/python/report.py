
import argparse
import json
import pathlib
import logging

import os.path
import math

import uvloop

DATA = []

parser = argparse.ArgumentParser(description='Stress test micronaut')
parser.add_argument(
  '--dataDir',
  type=pathlib.Path,
  default='/tmp/data'
)
parser.add_argument(
  'outDir',
  type=pathlib.Path,
  default="/tmp/report",
)



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
