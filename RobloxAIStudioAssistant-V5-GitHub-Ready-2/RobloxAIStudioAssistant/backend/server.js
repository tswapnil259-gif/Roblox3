import express from "express";
import rateLimit from "express-rate-limit";

const app = express();
app.use(express.json({ limit: "2mb" }));

const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false
});
app.use("/api/", limiter);

const RELAY_AUTH_TOKEN = process.env.RELAY_AUTH_TOKEN || "";

function requireRelayToken(req, res, next) {
  if (!RELAY_AUTH_TOKEN) return next();
  const token = (req.headers.authorization || "").replace("Bearer ", "");
  if (token !== RELAY_AUTH_TOKEN) {
    return res.status(401).json({ error: "Invalid relay bearer token" });
  }
  next();
}

app.get("/api/health", requireRelayToken, (_req, res) => {
  res.json({
    ok: true,
    configuredProviders: {
      OPENAI: Boolean(process.env.OPENAI_API_KEY),
      ANTHROPIC: Boolean(process.env.ANTHROPIC_API_KEY),
      DEEPSEEK: Boolean(process.env.DEEPSEEK_API_KEY),
      OPENROUTER: Boolean(process.env.OPENROUTER_API_KEY)
    }
  });
});

app.post("/api/chat", requireRelayToken, async (req, res) => {
  try {
    const { provider, model, systemPrompt, messages } = req.body || {};
    if (!provider || !model || !Array.isArray(messages)) {
      return res.status(400).json({ error: "provider, model, and messages are required" });
    }

    if (provider === "ANTHROPIC") {
      const key = process.env.ANTHROPIC_API_KEY;
      if (!key) return res.status(401).json({ error: "ANTHROPIC_API_KEY not configured on server" });

      const normalized = [];
      for (const m of messages) {
        const role = m.role === "assistant" ? "assistant" : "user";
        if (normalized.length === 0 && role !== "user") continue;
        if (normalized.length > 0 && normalized[normalized.length - 1].role === role) {
          normalized[normalized.length - 1].content += "\n\n" + m.content;
        } else {
          normalized.push({ role, content: m.content });
        }
      }

      const upstream = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "x-api-key": key,
          "anthropic-version": "2023-06-01",
          "content-type": "application/json"
        },
        body: JSON.stringify({
          model,
          max_tokens: 4096,
          system: systemPrompt || "",
          messages: normalized
        })
      });
      const data = await upstream.json();
      if (!upstream.ok) return res.status(upstream.status).json(data);
      const text = (data.content || []).filter((c) => c.type === "text").map((c) => c.text).join("\n");
      return res.json({ content: text });
    }

    const configMap = {
      OPENAI: { url: "https://api.openai.com/v1/chat/completions", key: process.env.OPENAI_API_KEY },
      DEEPSEEK: { url: "https://api.deepseek.com/chat/completions", key: process.env.DEEPSEEK_API_KEY },
      OPENROUTER: { url: "https://openrouter.ai/api/v1/chat/completions", key: process.env.OPENROUTER_API_KEY }
    };

    const target = configMap[provider];
    if (!target) return res.status(400).json({ error: `Unsupported provider: ${provider}` });
    if (!target.key) return res.status(401).json({ error: `${provider} key not configured on server` });

    const payloadMessages = systemPrompt
      ? [{ role: "system", content: systemPrompt }, ...messages]
      : messages;

    const upstream = await fetch(target.url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${target.key}`,
        "Content-Type": "application/json",
        ...(provider === "OPENROUTER" ? { "X-Title": "Roblox AI Studio Assistant" } : {})
      },
      body: JSON.stringify({ model, messages: payloadMessages })
    });
    const data = await upstream.json();
    if (!upstream.ok) return res.status(upstream.status).json(data);
    const text = data.choices?.[0]?.message?.content || "";
    return res.json({ content: text });
  } catch (err) {
    return res.status(500).json({ error: String(err) });
  }
});

const port = process.env.PORT || 8787;
app.listen(port, () => console.log(`Relay running on :${port}`));
