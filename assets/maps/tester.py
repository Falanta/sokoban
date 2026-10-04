from collections import deque
import json
import os
import sys


def parse_sokoban_level(lines):
  """Парсит уровень, определяя проходимые зоны (пол) и препятствия."""
  meta = {}
  map_lines = []
  spawn_actions = []

  for line in lines:
    line = line.rstrip("\n\r")
    if not line:
      continue
    if line.strip().startswith("{"):
      data = json.loads(line.strip())
      if "assets" in data:
        meta = data
      elif data.get("action") == "spawn":
        spawn_actions.append(data)
    else:
      map_lines.append(line)

  assets = meta.get("assets", {})
  spawn_pos = tuple(meta.get("spawn_pos", (0, 0)))

  floors = set()

  # Заполняем только те клетки, которые являются полом
  for r_idx, row in enumerate(map_lines):
    for c_idx, char in enumerate(row):
      pos = (c_idx, r_idx)
      tile_type = assets.get(char, "")
      # Если символ есть в assets и это НЕ стена — это пол
      if char in assets and "wall" not in tile_type.lower():
        floors.add(pos)

  crates = set()
  anchors = set()

  for act in spawn_actions:
    pos = tuple(act["pos"])
    if act["type"] == "crate":
      crates.add(pos)
    elif act["type"] == "anchor":
      anchors.add(pos)

  return floors, tuple(sorted(crates)), tuple(sorted(anchors)), spawn_pos


# Направления: ( dx, dy, символ стрелки )
MOVE_DIRS = {
    "UP": (0, -1, "↑"),
    "DOWN": (0, 1, "↓"),
    "LEFT": (-1, 0, "←"),
    "RIGHT": (1, 0, "→"),
}


def solve_sokoban(floors, initial_player, initial_crates, anchors):
  """Решает уровень Сокобан методом BFS."""
  initial_state = (initial_player, initial_crates)
  queue = deque([(initial_state, "")])
  visited = {initial_state}
  anchor_set = set(anchors)

  while queue:
    (player_pos, crates), path = queue.popleft()

    # Проверка победы: все ящики на якорях
    if all(c in anchor_set for c in crates):
      return path

    crates_set = set(crates)

    for action_name, (dx, dy, arrow) in MOVE_DIRS.items():
      next_player_pos = (player_pos[0] + dx, player_pos[1] + dy)

      # 1. Игрок может ходить только по полу
      if next_player_pos not in floors:
        continue

      next_crates = set(crates)
      new_path = path + arrow

      # 2. Проверяем, есть ли на следующей позиции ящик
      if next_player_pos in crates_set:
        next_crate_pos = (next_player_pos[0] + dx, next_player_pos[1] + dy)

        # Ящик тоже может двигаться только по полу и не на другой ящик
        if next_crate_pos not in floors or next_crate_pos in crates_set:
          continue

        next_crates.remove(next_player_pos)
        next_crates.add(next_crate_pos)

      next_state = (next_player_pos, tuple(sorted(next_crates)))

      if next_state not in visited:
        visited.add(next_state)
        queue.append((next_state, new_path))

  return None


if __name__ == "__main__":
  level_name = input(
      "Введите название уровня (например, hard_02): "
  ).strip()
  filename = f"{level_name}.map"

  if not os.path.exists(filename):
    print(f"Ошибка: Файл '{filename}' не найден в текущей директории!")
    sys.exit(1)

  print(f"Читаем уровень из файла {filename}...")
  try:
    with open(filename, "r", encoding="utf-8") as f:
      level_data = f.readlines()
  except Exception as e:
    print(f"Ошибка при чтении файла: {e}")
    sys.exit(1)

  print("Парсим уровень...")
  floors, crates, anchors, spawn_pos = parse_sokoban_level(level_data)

  print(
      f"Проходимых клеток пола: {len(floors)}, Ящиков: {len(crates)},"
      f" Якорей: {len(anchors)}"
  )
  print(f"Позиция игрока: {spawn_pos}")
  print("Запускаем поиск решения (BFS)...")

  solution = solve_sokoban(floors, spawn_pos, crates, anchors)

  if solution:
    print("\nПобеда! Самый быстрый путь найден:")
    print(solution)
  else:
    print("\nРешение не найдено (возможно, уровень неразрешим).")