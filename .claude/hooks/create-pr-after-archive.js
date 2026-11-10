// PostToolUse hook helper (invoked by post-archive-pr.sh) after a `git commit`
// whose message matches the final archive commit (`docs(openspec): archive <change> and sync specs`,
// per CLAUDE.md "Ramas y commits"). Pushes the branch and opens the PR to develop with
// title/body derived from the archived change's proposal.md, so this manual step in the
// workflow can't be forgotten.
const { execSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

function run(cmd) {
  return execSync(cmd, { encoding: "utf8" }).trim();
}

function tryRun(cmd) {
  try {
    return run(cmd);
  } catch (err) {
    return null;
  }
}

function section(markdown, heading) {
  const re = new RegExp(`##\\s+${heading}\\s*\\n([\\s\\S]*?)(?=\\n##\\s|$)`, "i");
  const found = markdown.match(re);
  return found ? found[1].trim() : "";
}

function prTypeFor(changeName) {
  if (/^(add|new)-/.test(changeName)) return "feat";
  if (/^fix-/.test(changeName)) return "fix";
  if (/^(remove|retire|drop)-/.test(changeName)) return "refactor";
  return "feat";
}

function main() {
  const message = tryRun("git log -1 --pretty=%B");
  if (!message) return;

  const match = message.match(/^docs\(openspec\):\s*archive\s+(\S+)\s+and sync specs/i);
  if (!match) return;
  const changeName = match[1];

  const branch = tryRun("git branch --show-current");
  if (!branch || branch === "develop" || branch === "master") return;

  const existingPr = tryRun(`gh pr list --head "${branch}" --json number --jq ".[0].number"`);
  if (existingPr) {
    console.log(`Hook post-archive: ya existe la PR #${existingPr} para '${branch}'; no se crea otra.`);
    return;
  }

  const archiveDir = path.join("openspec", "changes", "archive");
  let proposalPath = null;
  if (fs.existsSync(archiveDir)) {
    const dirs = fs
      .readdirSync(archiveDir)
      .filter((d) => d.endsWith(`-${changeName}`))
      .sort();
    const latest = dirs[dirs.length - 1];
    if (latest) {
      const candidate = path.join(archiveDir, latest, "proposal.md");
      if (fs.existsSync(candidate)) proposalPath = candidate;
    }
  }

  let whatChanges = "";
  if (proposalPath) {
    const content = fs.readFileSync(proposalPath, "utf8");
    whatChanges = section(content, "What Changes");
  }

  const title = `${prTypeFor(changeName)}: ${changeName.replace(/-/g, " ")}`;
  const bodyParts = [];
  bodyParts.push(
    whatChanges ? `## Resumen\n\n${whatChanges}` : `## Resumen\n\nCambio de OpenSpec \`${changeName}\`.`
  );
  bodyParts.push(
    "## Plan de pruebas\n\n- [x] `./gradlew check` en verde (hook de pre-commit).\n- [x] `./gradlew koverVerify` en verde.\n- CI de la PR."
  );
  bodyParts.push("🤖 Generated with [Claude Code](https://claude.com/claude-code)");
  const body = bodyParts.join("\n\n");

  if (tryRun("git push -u origin HEAD") === null) {
    console.error("Hook post-archive: fallo al hacer push de la rama; no se crea la PR. Revísalo manualmente.");
    return;
  }

  const bodyFile = path.join(os.tmpdir(), `pr-body-${Date.now()}.md`);
  fs.writeFileSync(bodyFile, body, "utf8");
  try {
    const prUrl = run(`gh pr create --base develop --title "${title.replace(/"/g, '\\"')}" --body-file "${bodyFile}"`);
    console.log(`Hook post-archive: PR creada -> ${prUrl}`);
  } catch (err) {
    console.error(`Hook post-archive: fallo al crear la PR con gh: ${err.message}`);
  } finally {
    fs.unlinkSync(bodyFile);
  }
}

main();
