const API_URL = "http://127.0.0.1:8000";


export async function sendAudio(audioBlob) {

    const formData = new FormData();

    formData.append(
        "file",
        audioBlob,
        "student.webm"
    );


    const response = await fetch(
        `${API_URL}/tutor`,
        {
            method: "POST",
            body: formData
        }
    );


    if (!response.ok) {

        const errorText =
            await response.text();

        throw new Error(
            `Tutor API error: ${response.status} ${errorText}`
        );
    }


    return await response.json();
}