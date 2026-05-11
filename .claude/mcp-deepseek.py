#!/usr/bin/env python3
"""MCP server per a DeepSeek V4 — implementació rutinària delegada."""

import os
import dotenv
from mcp.server import Server, NotificationOptions
from mcp.server.models import InitializationOptions
import mcp.server.stdio
from mcp.types import Tool, TextContent
from openai import OpenAI

dotenv.load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

api_key = os.environ.get("DEEPSEEK_API_KEY")
if not api_key:
    msg = "DEEPSEEK_API_KEY no trobada. Posa-la a .env o exporta-la com a variable d'entorn."
    raise RuntimeError(msg)

client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com/v1")

server = Server("deepseek")


@server.list_tools()
async def list_tools():
    return [
        Tool(
            name="deepseek-generate",
            description=(
                "Genera codi rutinari seguint especificacions aprovades. "
                "Usa'l per a: entitats JPA, repositoris, DTOs, serveis CRUD, "
                "components Next.js, tests, i qualsevol implementació mecànica. "
                "NO l'usis per a decisions d'arquitectura o seguretat."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "task": {
                        "type": "string",
                        "description": "Descripció clara de la tasca d'implementació",
                    },
                    "spec": {
                        "type": "string",
                        "description": "Contingut de l'spec o referència al mòdul",
                    },
                    "context": {
                        "type": "string",
                        "description": "Codi existent, exemples de patró, o altres referències",
                    },
                    "language": {
                        "type": "string",
                        "enum": ["java", "typescript", "python", "yaml", "dockerfile", "sql"],
                        "description": "Llenguatge de sortida",
                    },
                },
                "required": ["task"],
            },
        )
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict):
    if name != "deepseek-generate":
        raise ValueError(f"Eina desconeguda: {name}")

    task = arguments.get("task", "")
    spec = arguments.get("spec", "")
    context = arguments.get("context", "")
    language = arguments.get("language", "typescript")

    prompt = f"""Ets un enginyer de software sènior. Implementa la següent tasca de manera precisa i completa.

TASCA:
{task}
"""

    if spec:
        prompt += f"\n\nSPEC:\n{spec}"
    if context:
        prompt += f"\n\nCONTEXT:\n{context}"

    prompt += f"\n\nGenera codi {language}. Només codi — ni explicacions ni markdown extra."

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {
                "role": "system",
                "content": "Ets un enginyer de software. Genera codi net, funcional i sense explicacions.",
            },
            {"role": "user", "content": prompt},
        ],
        temperature=0.1,
    )

    result = response.choices[0].message.content or ""

    return [TextContent(type="text", text=result)]


async def main():
    async with mcp.server.stdio.stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="deepseek",
                server_version="0.1.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    import asyncio

    asyncio.run(main())
