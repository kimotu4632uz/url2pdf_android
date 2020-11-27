use scraper::{Html, Selector};
use image::GenericImageView;
use mime_guess::mime;
use itertools::Itertools;

use std::collections::HashMap;

pub fn get_urls(html: String) -> String {
    let selector = Selector::parse("a,img").unwrap();

    let mut urls = Html::parse_document(&html)
        .select(&selector)
        .filter_map(|elem| {
            match elem.value().name() {
                "a" => elem.value().attr("href"),
                "img" => elem.value().attr("src"),
                _ => None,
            }
            .map(String::from)
        })
        .filter(|url| {
            match mime_guess::from_path(url).first() {
                Some(mime) =>
                    if mime == mime::IMAGE_JPEG || mime == mime::IMAGE_PNG { true } else { false },
                None => false,
            }
        })
        .collect_vec();
    
    let mut set: HashMap<_,_> = urls.clone().into_iter().map(|x| (x.clone(), x.split("/").last().unwrap_or("").split(".").next().unwrap_or("").to_string())).collect();
    urls.reverse();

    let mut remove_list = Vec::new();
    for (key, val) in &set {
        for (key_t, val_t) in &set {
            if key != key_t {
                if val_t.contains(val) {
                    remove_list.push(key.clone());
                }
            }
        }
    }

    for item in remove_list {
        set.remove(&item);
    }

    let mut result = urls.into_iter().filter(|x| set.remove(x).is_some()).collect_vec();
    result.reverse();
    result.join("\n")
}

pub fn filter_img(imgs: Vec<&[u8]>) -> anyhow::Result<Vec<&[u8]>> {
    let mut result = Vec::new();

    for img in imgs {
        let (height, _) = image::load_from_memory(img)?.dimensions();
        if height > 700 {
            result.push(img);
        }
    }

    Ok(result)
}