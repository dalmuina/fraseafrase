// Reads the PreToolUse hook's stdin JSON and prints the Bash command with any
// heredoc bodies stripped out, so substring matches (e.g. "git push") don't
// false-trigger on literal text inside a commit message passed via heredoc.
let data = "";
process.stdin.on("data", (chunk) => {
  data += chunk;
});
process.stdin.on("end", () => {
  let command = "";
  try {
    const parsed = JSON.parse(data);
    command = (parsed.tool_input && parsed.tool_input.command) || "";
  } catch (err) {
    command = "";
  }
  const withoutHeredocs = command.replace(
    /<<-?\s*(['"]?)([A-Za-z_][A-Za-z0-9_]*)\1[\s\S]*?\n[ \t]*\2(?=\n|$)/g,
    ""
  );
  process.stdout.write(withoutHeredocs);
});
