import argparse
import io
import pathlib
import logging
import json
import os
import base64

import numpy as np
import matplotlib.pyplot as plt
from jinja2 import Template

TEMPLATE = Template("""
<!DOCTYPE html>

<html>
    <body>
        <h1>Experiment results</h1>
        <h2>Statistics</h2>
        <table>
            <tr>
                <th>Duration</th>
                <th>Queries sent</th>
                <th>QPS</th>
                <th>P50 (us)</th>
                <th>P90 (us)</th>
                <th>P99 (us)</th>
                <th>Error rate</th>
            </tr>
            {% for row in stats %}
                <tr>
                    <td>{{ row ['duration'] }}</td>
                    <td>{{ row ['queries'] }}</td>
                    <td>{{ row ['qps']|round(2) }}</td>
                    <td>{{ row ['p_50']|round(3) }}</td>
                    <td>{{ row ['p_90']|round(3) }}</td>
                    <td>{{ row ['p_99']|round(3) }}</td>
                    <td>{{ row ['error_rate']|round(3) }}</td>
                </tr>
            {% endfor %}
        </table>

        <h2>Plots</h2>
        {% for img in plots %}
            <h3>{{ img['title'] }}</h3>
            <img src="data:image/png;base64,{{- img['data'].decode() -}}" alt="{{- img['title'] -}}" />
        {% endfor %}
    </body>
</html>
""")


parser = argparse.ArgumentParser(description='report generator')
parser.add_argument(
  '--dataDir',
  type=pathlib.Path,
  default='/tmp/data'
)
parser.add_argument(
  '--outDir',
  type=pathlib.Path,
  default='/tmp/report'
)


def get_stats(experiment_data):
  duration = experiment_data['duration']
  requests_sent = len(experiment_data['data'])
  actual_qps = requests_sent / duration

  results = []
  durations = []

  for entry in experiment_data['data']:
    results.append(entry['success'])
    durations.append(entry['time_us'])

  p_50 = np.percentile(durations, 50)
  p_90 = np.percentile(durations, 90)
  p_99 = np.percentile(durations, 99)

  rates = dict(zip(*np.unique(results, return_counts=True)))
  succeeded = rates.get(True, 0)
  failed = rates.get(False, 0)
  error_rate = failed / (succeeded + failed)

  return {
    'duration': duration,
    'queries': requests_sent,
    'qps': actual_qps,
    'p_50': p_50,
    'p_90': p_90,
    'p_99': p_99,
    'error_rate': error_rate
  }


def get_plots(all_stats):
  actual_qps = [row['qps'] for row in all_stats]
  res = []

  error_rates = [row['error_rate'] for row in all_stats]
  img_errors = io.BytesIO()
  plt.plot(actual_qps, error_rates)
  plt.xlabel('Query rate')
  plt.ylabel('Error rate')
  plt.savefig(img_errors, format='png')
  res.append({
    'title': 'Error rate',
    'data': base64.encodebytes(img_errors.getvalue())
  })
  plt.clf()

  p_50 = [row['p_50'] for row in all_stats]
  img_p50 = io.BytesIO()
  plt.plot(actual_qps, p_50, 'o')
  plt.xlabel('Query rate')
  plt.ylabel('P50 (us)')
  plt.savefig(img_p50, format='png')
  res.append({
    'title': '50 Percentile',
    'data': base64.encodebytes(img_p50.getvalue())
  })
  plt.clf()

  p_90 = [row['p_90'] for row in all_stats]
  img_p90 = io.BytesIO()
  plt.plot(actual_qps, p_90, 'o')
  plt.xlabel('Query rate')
  plt.ylabel('P90 (us)')
  plt.savefig(img_p90, format='png')
  res.append({
    'title': '90 Percentile',
    'data': base64.encodebytes(img_p90.getvalue())
  })
  plt.clf()

  p_99 = [row['p_99'] for row in all_stats]
  img_p99 = io.BytesIO()
  plt.plot(actual_qps, p_99, 'o')
  plt.xlabel('Query rate')
  plt.ylabel('P99 (us)')
  plt.savefig(img_p99, format='png')
  res.append({
    'title': '99 Percentile',
    'data': base64.encodebytes(img_p99.getvalue())
  })
  plt.clf()

  return res


if __name__ == '__main__':
  logging.getLogger().setLevel(logging.INFO)

  args = parser.parse_args()
  data_dir = args.dataDir
  out_dir = args.outDir

  stats = []
  for experiment in os.listdir(data_dir):
    with open(os.path.join(data_dir, experiment)) as file:
      data = json.load(file)

    stats.append(get_stats(data))

  plots = get_plots(stats)
  rendered = TEMPLATE.render(
    stats=stats,
    plots=plots,
  )

  os.makedirs(out_dir, exist_ok=True)
  out_path = os.path.join(out_dir, 'report.html')
  with open(out_path, 'w+') as file:
    file.write(rendered)
  os.chmod(out_path, 0o777)
