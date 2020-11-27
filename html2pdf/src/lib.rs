pub mod html2img;
pub mod img2pdf;
pub mod png;

pub fn img_vec2pdf(imgs: Vec<&[u8]>, link: String) -> anyhow::Result<Vec<u8>> {
    let img_result = html2img::filter_img(imgs)?;

    let mut pdf = img2pdf::Pdf::new();
    let mut first = true;

    for img in img_result {
        let page_id = pdf.add_image(img)?;
        if first {
            pdf.add_link(&link, page_id)?;
            first = false;
        }
    }

    let mut result = Vec::new();
    pdf.save(&mut result)?;
    Ok(result)
}

#[cfg(target_os = "android")]
pub mod android {
    use jni::objects::{JClass, JString};
    use jni::sys::{jstring, jbyteArray};
    use jni::JNIEnv;

    #[no_mangle]
    pub extern "system" fn Java_com_kimotu_url2pdf_Rustlib_geturls(
        env: JNIEnv,
        _: JClass,
        input: JString
    ) -> jstring {
        let input: String = env.get_string(input).expect("invalid string").into();
        let output = env.new_string(crate::html2img::get_urls(input)).expect("unable to create java string");
        output.into_inner()
    }

    #[no_mangle]
    pub extern "system" fn Java_com_kimotu_url2pdf_Rustlib_img2pdf(
        env: JNIEnv,
        _: JClass,
        bytes: jbyteArray,
        pos: JString,
        link: JString
    ) -> jbyteArray {
        let src = env.convert_byte_array(bytes).expect("unable to get bytearray");
        let pos: String = env.get_string(pos).expect("invalid string").into();
        let link: String = env.get_string(link).expect("invalid string").into();
        let sizes: Vec<u32> = pos.lines().map(str::parse).collect::<Result<_, _>>().expect("invalid format string");

        let mut src_deref = &src[..];
        let mut imgs = Vec::new();
        for size in sizes {
            let (target, left) = src_deref.split_at(size as usize);
            imgs.push(target);
            src_deref = left;
        }

        let result = crate::img_vec2pdf(imgs, link).expect("unable to make pdf");
        env.byte_array_from_slice(&result).expect("unable to make jbytearray")
    }
}