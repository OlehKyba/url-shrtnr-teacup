use clap::{App, Arg, ArgMatches};
use log::{info, LevelFilter};
use rand::{distributions::Alphanumeric, Rng, RngCore};
use reqwest::blocking::Response;
use reqwest::redirect::Policy;
use reqwest::Error;
use serde::{Deserialize, Serialize};
use simple_logger::SimpleLogger;
use std::collections::HashMap;
use std::path::Path;
use std::sync::mpsc::{channel, RecvTimeoutError, Sender};
use std::time::Duration;
use std::{env, fs, thread, time};

/// blocking wait until server will be ready to accept requests
fn wait_for_startup(host: &str) {
    loop {
        let response = reqwest::blocking::get(host);
        match response {
            Ok(resp) => {
                info!("Server is up and ready to be tested");
                break;
            }
            Err(err) => {
                info!("Waiting for server startup");
                thread::sleep(time::Duration::from_secs(1));
            }
        }
    }
}

#[derive(Deserialize, Debug)]
struct TokenResponse {
    access_token: String,
}

#[derive(Serialize, Debug)]
struct Experiment {
    duration: u64,
    rps: u64,
    data: Vec<ExperimentEntry>,
}

#[derive(Serialize, Debug)]
struct ExperimentEntry {
    success: bool,
    time_us: u128,
}

fn random_string(size: usize) -> String {
    rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(size)
        .map(char::from)
        .collect()
}

fn get_token_and_shortened_url(host: &str) -> (String, String) {
    let email = format!("{}@example.com", random_string(10));
    let mut sign_up_data = HashMap::new();
    sign_up_data.insert("email", email.as_str());
    sign_up_data.insert("password", "test");

    let client = reqwest::blocking::Client::new();
    let sign_up_res = client
        .post(format!("{}/users/signup", host))
        .header("Content-Type", "application/json")
        .json(&sign_up_data)
        .send()
        .ok()
        .unwrap();
    assert!(sign_up_res.status().is_success());

    let mut sign_in_data = HashMap::new();
    sign_in_data.insert("username", email.as_str());
    sign_in_data.insert("password", "test");
    let sign_in_res = client
        .post(format!("{}/login", host))
        .header("Content-Type", "application/json")
        .json(&sign_in_data)
        .send()
        .ok()
        .unwrap();
    assert!(sign_in_res.status().is_success());
    let token: TokenResponse = sign_in_res.json().unwrap();
    info!("Token: {:?}", token.access_token);

    let mut shorten_data = HashMap::new();
    shorten_data.insert("url", "https://google.com");
    let shorten_res = client
        .post(format!("{}/urls/shorten", host))
        .header("Content-Type", "application/json")
        .bearer_auth(&token.access_token)
        .json(&shorten_data)
        .send()
        .ok()
        .unwrap();
    assert!(shorten_res.status().is_success());
    let text = shorten_res.text().unwrap();
    let parts = text.split("/").collect::<Vec<&str>>();
    let alias = parts[parts.len() - 1];
    info!("Alias: {:?}", alias);

    return (token.access_token.to_string(), alias.to_string());
}

fn experiment_redirect(tx: Sender<ExperimentEntry>, path: String) {
    let mut success = true;
    let client = reqwest::blocking::ClientBuilder::new()
        .redirect(Policy::none())
        .build()
        .unwrap();
    let exp_start = time::SystemTime::now();
    let resp = client.get(path).send();
    match resp {
        Ok(r) => success = r.status().is_redirection(),
        Err(e) => {
            success = false;
        }
    }
    let end = exp_start.elapsed().unwrap().as_micros();
    let entry = ExperimentEntry {
        success,
        time_us: end,
    };
    info!("{:?}", entry);
    tx.send(entry);
}

fn experiment_sign_up(tx: Sender<ExperimentEntry>, path: String) {
    let mut success = true;
    let email = format!("{}@example.com", random_string(10));
    let mut sign_up_data = HashMap::new();
    sign_up_data.insert("email", email.as_str());
    sign_up_data.insert("password", "test");

    let client = reqwest::blocking::Client::new();
    let exp_start = time::SystemTime::now();
    let resp = client
        .post(path)
        .header("Content-Type", "application/json")
        .json(&sign_up_data)
        .send();

    match resp {
        Ok(r) => success = r.status().is_success(),
        Err(e) => {
            success = false;
        }
    }
    let end = exp_start.elapsed().unwrap().as_micros();
    let entry = ExperimentEntry {
        success,
        time_us: end,
    };
    info!("{:?}", entry);
    tx.send(entry);
}

fn experiment_sign_in(tx: Sender<ExperimentEntry>, path: String) {
    let mut success = true;
    let email = format!("{}@example.com", random_string(10));
    let mut sign_in_data = HashMap::new();
    sign_in_data.insert("username", email.as_str());
    sign_in_data.insert("password", "test");

    let client = reqwest::blocking::Client::new();
    let exp_start = time::SystemTime::now();
    let resp = client
        .post(path)
        .header("Content-Type", "application/json")
        .json(&sign_in_data)
        .send();

    match resp {
        Ok(r) => success = r.status().is_success(),
        Err(e) => {
            success = false;
        }
    }
    let end = exp_start.elapsed().unwrap().as_micros();
    let entry = ExperimentEntry {
        success,
        time_us: end,
    };
    info!("{:?}", entry);
    tx.send(entry);
}

fn main() {
    SimpleLogger::new()
        .with_level(LevelFilter::Info)
        .init()
        .unwrap();
    let args = App::new("Load generator")
        .version("0.1.0")
        .author("Oleksandr Korienev <alexkorienev@gmail.com>")
        .arg(
            Arg::with_name("host")
                .short("h")
                .long("host")
                .takes_value(true)
                .default_value("http://localhost:8080")
                .help("host to test"),
        )
        .arg(
            Arg::with_name("rps")
                .short("r")
                .long("rps")
                .takes_value(true)
                .default_value("1")
                .help("requests per second"),
        )
        .arg(
            Arg::with_name("duration")
                .short("d")
                .long("duration")
                .takes_value(true)
                .default_value("30")
                .help("duration in seconds"),
        )
        .arg(
            Arg::with_name("out_dir")
                .short("o")
                .long("out_dir")
                .takes_value(true)
                .default_value("/tmp/data")
                .help("directory to write result to"),
        )
        .get_matches();
    let host = args.value_of("host").unwrap();
    let duration: u64 = args.value_of("duration").unwrap().parse::<u64>().unwrap();
    let rps: u64 = args.value_of("rps").unwrap().parse::<u64>().unwrap();
    let out_dir = args.value_of("out_dir").unwrap();

    wait_for_startup(host);
    info!(
        "Starting test of {} with {} rps for {} seconds",
        host, rps, duration
    );
    let (token, alias) = get_token_and_shortened_url(host);
    let (tx, rx) = channel::<ExperimentEntry>();
    let start = time::SystemTime::now();
    loop {
        let tx = tx.clone();
        let path_redirect = format!("{}/r/{}", host, alias);
        let path_sign_up = format!("{}/users/signup", host);
        let path_sign_in = format!("{}/login", &host);
        thread::spawn(move || {
            let random = rand::thread_rng().next_u32();
            if random > 0x0fffffff {
                experiment_redirect(tx, path_redirect)
            } else if random & 0x00ffffff != 0 {
                experiment_sign_up(tx, path_sign_up);
            } else {
                experiment_sign_in(tx, path_sign_in);
            }
        });

        if start.elapsed().unwrap().as_secs() >= duration {
            break;
        }
        thread::sleep(Duration::from_micros(1_000_000 / rps));
    }
    info!("Experiment ended, collecting results");
    let mut experiment = Experiment {
        duration,
        rps,
        data: Vec::with_capacity((duration * rps) as usize),
    };
    loop {
        // heuristic, we think that if we have no data for 10 seconds
        // than all threads ended their work
        match rx.recv_timeout(Duration::from_secs(10)) {
            Ok(e) => experiment.data.push(e),
            Err(_) => break,
        }
    }

    fs::create_dir_all(out_dir);
    let path = Path::new(out_dir).join(format!("{}-{}.json", duration, rps));
    info!("Saving result to {:?}", path);
    serde_json::to_writer(&fs::File::create(path).unwrap(), &experiment);
    info!("Result saved");
}
