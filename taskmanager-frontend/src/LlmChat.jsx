import React, { useState } from "react";

function LlmChat() {
  const [prompt, setPrompt] = useState("");
  const [response, setResponse] = useState(""); // This holds the extracted LLM response
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handlePromptSubmit = async () => {
    // ... (your existing fetch logic) ...
    try {
      setError("");
      setResponse("");
      const backendUrl = "http://localhost:8080/api/ask"; // Make sure this is 'api/ask' now
      const res = await fetch(backendUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: prompt }),
      });
      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(
          errorData.message || `HTTP error! Status: ${res.status}`
        );
      }
      const data = await res.json();
      setResponse(data.response); // This correctly extracts just the 'response' field
      setPrompt("");
    } catch (err) {
      console.error("Failed to fetch LLM response:", err);
      setError(err.message || "An unknown error occurred.");
    } finally {
      setIsLoading(false);
    }
  };

  // YOU NEED TO FIND THIS 'return' STATEMENT IN YOUR LLMChat.js FILE
  return (
    <div
      style={{
        padding: "20px",
        maxWidth: "800px",
        margin: "auto",
        fontFamily: "Arial, sans-serif",
      }}
    >
      <h1>Agentic AI Chat</h1>
      <textarea
        placeholder="Enter your prompt here..."
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        rows="5"
        style={{
          width: "100%",
          padding: "10px",
          marginBottom: "10px",
          border: "1px solid #ccc",
        }}
      />
      <button
        onClick={handlePromptSubmit}
        disabled={isLoading || !prompt.trim()}
        style={{
          padding: "10px 20px",
          backgroundColor: "#007bff",
          color: "white",
          border: "none",
          borderRadius: "4px",
          cursor: "pointer",
          opacity: isLoading || !prompt.trim() ? 0.6 : 1,
        }}
      >
        {isLoading ? "Sending..." : "Send Prompt"}
      </button>

      {error && (
        <div
          style={{
            color: "red",
            marginTop: "15px",
            border: "1px solid red",
            padding: "10px",
            borderRadius: "4px",
          }}
        >
          Error: {error}
        </div>
      )}

      {/* THIS IS THE PART YOU NEED TO CONFIRM IN YOUR CODE */}
      {response && ( // This checks if the 'response' state variable has any content
        <div
          style={{
            marginTop: "20px",
            padding: "15px",
            border: "1px solid #ddd",
            borderRadius: "4px",
            backgroundColor: "#f9f9f9",
          }}
        >
          <h2>LLM Response:</h2>
          <p>{response}</p>{" "}
          {/* <--- THIS IS WHERE THE 'response' STATE IS RENDERED */}
        </div>
      )}
      {/* END OF THE PART TO CONFIRM */}
    </div>
  );
}

export default LlmChat;
