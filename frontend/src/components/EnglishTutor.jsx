import { useRef, useState } from "react";
import { sendAudio } from "../services/tutorService";


function EnglishTutor() {

    const mediaRecorderRef = useRef(null);

    const audioChunksRef = useRef([]);


    const [isRecording, setIsRecording] =
        useState(false);

    const [isProcessing, setIsProcessing] =
        useState(false);

    const [result, setResult] =
        useState(null);

    const [error, setError] =
        useState("");


    async function startRecording() {

        setError("");
        setResult(null);

        try {

            const stream =
                await navigator.mediaDevices
                    .getUserMedia({
                        audio: true
                    });


            audioChunksRef.current = [];


            const recorder =
                new MediaRecorder(stream);


            mediaRecorderRef.current =
                recorder;


            recorder.ondataavailable =
                (event) => {

                    if (event.data.size > 0) {

                        audioChunksRef.current.push(
                            event.data
                        );

                    }
                };


            recorder.onstop =
                async () => {

                    const audioBlob =
                        new Blob(
                            audioChunksRef.current,
                            {
                                type: "audio/webm"
                            }
                        );


                    stream
                        .getTracks()
                        .forEach(
                            track =>
                                track.stop()
                        );


                    await processAudio(
                        audioBlob
                    );
                };


            recorder.start();

            setIsRecording(true);

        } catch (err) {

            console.error(err);

            setError(
                "Unable to access your microphone."
            );
        }
    }


    function stopRecording() {

        if (
            mediaRecorderRef.current &&
            isRecording
        ) {

            mediaRecorderRef.current.stop();

            setIsRecording(false);
        }
    }


    async function processAudio(
        audioBlob
    ) {

        try {

            setIsProcessing(true);

            const response =
                await sendAudio(
                    audioBlob
                );

            setResult(response);

        } catch (err) {

            console.error(err);

            setError(
                err.message ||
                "Unable to process your speech."
            );

        } finally {

            setIsProcessing(false);
        }
    }


    function playCorrection() {

        if (!result) {
            return;
        }


        const audio =
            new Audio(
                `http://127.0.0.1:8000${result.audio_url}`
            );


        audio.play();
    }


    return (

        <div className="tutor-container">

            <h1>
                🗣️ English Tutor
            </h1>

            <p>
                Speak naturally and improve your English.
            </p>


            <button
                className={
                    isRecording
                        ? "recording"
                        : ""
                }

                disabled={isProcessing}

                onClick={
                    isRecording
                        ? stopRecording
                        : startRecording
                }
            >

                {isRecording
                    ? "⏹️ Stop Speaking"
                    : "🎤 Start Speaking"
                }

            </button>


            {isRecording && (

                <p>
                    🔴 Recording...
                </p>

            )}


            {isProcessing && (

                <p>
                    🤔 AI is checking your English...
                </p>

            )}


            {error && (

                <p>
                    ❌ {error}
                </p>

            )}


            {result && (

                <div>

                    <h2>
                        You said
                    </h2>

                    <p>
                        {result.transcription}
                    </p>


                    <h2>
                        ✨ Correct sentence
                    </h2>

                    <p>
                        {
                            result.correction.corrected
                        }
                    </p>


                    <h2>
                        💡 Why?
                    </h2>

                    <p>
                        {
                            result.correction.explanation
                        }
                    </p>


                    <h2>
                        🎯 Practice
                    </h2>

                    <p>
                        {
                            result.correction.practice
                        }
                    </p>


                    <p>
                        👏{" "}
                        {
                            result.correction.encouragement
                        }
                    </p>


                    <button
                        onClick={playCorrection}
                    >
                        🔊 Listen to Correction
                    </button>

                </div>

            )}

        </div>
    );
}


export default EnglishTutor;