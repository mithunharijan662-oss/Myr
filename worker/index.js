export default {
  async fetch(request, env) {
    const headers = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers });
    }

    if (request.method === "GET") {
      return Response.json(
        { status: "MYRA backend online" },
        { headers }
      );
    }

    if (request.method !== "POST") {
      return Response.json({ error: "Method not allowed" }, {
        status: 405, headers
      });
    }

    try {
      if (!env.GEMINI_API_KEY) {
        return Response.json({ error: "API key not configured" }, {
          status: 500, headers
        });
      }

      const body = await request.json();
      const message = body.message;

      if (typeof message !== "string" ||
          !message.trim() || message.length > 4000) {
        return Response.json({ error: "Invalid message" }, {
          status: 400, headers
        });
      }

      const response = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{
                text: "You are MYRA, a helpful and friendly AI assistant. Reply naturally in the language used by the user."
              }]
            },
            contents: [{
              parts: [{ text: message }]
            }]
          })
        }
      );

      const data = await response.json();

      if (!response.ok) {
        return Response.json({
          error: "Gemini API request failed",
          details: data.error?.message || "Unknown error"
        }, { status: 502, headers });
      }

      const reply = data.candidates?.[0]?.content?.parts
        ?.map(part => part.text || "").join("") || "No reply received.";

      return Response.json({ reply }, { headers });

    } catch (error) {
      return Response.json({ error: "Request failed" }, {
        status: 500, headers
      });
    }
  }
};
