import os

tools_dir = r"c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\tools"
os.makedirs(tools_dir, exist_ok=True)

# 1. Tool.js
tool_js = """import { state, saveState } from '../state.js';
import { updateJSON, updateProperties, updateSelectionStats } from '../utils.js';

export class Tool {
    constructor(manager) {
        this.manager = manager;
    }
    
    onMouseDown(e, coords) {}
    onMouseMove(e, coords) {}
    onMouseUp(e, coords) {}
    onKeyDown(e) {}
    
    // Helper to snap to grid
    snap(val, size) {
        return Math.round(val / size) * size;
    }
}
"""

# 2. SelectTool.js
select_tool_js = """import { Tool } from './Tool.js';
import { state } from '../state.js';

export class SelectTool extends Tool {
    onMouseDown(e, coords) {
        state.selectedIds = [];
        state.isSelectingArea = true;
        state.selectionBox = { x1: coords.x, y1: coords.y, x2: coords.x, y2: coords.y };
    }

    onMouseMove(e, coords) {
        if (!state.isSelectingArea) return;
        
        state.selectionBox.x2 = coords.x;
        state.selectionBox.y2 = coords.y;
    }

    onMouseUp(e, coords) {
        if (!state.isSelectingArea) return;
        
        const r = {
            x: Math.min(state.selectionBox.x1, state.selectionBox.x2),
            y: Math.min(state.selectionBox.y1, state.selectionBox.y2),
            w: Math.abs(state.selectionBox.x2 - state.selectionBox.x1),
            h: Math.abs(state.selectionBox.y2 - state.selectionBox.y1)
        };

        const isMultiSelect = e.ctrlKey || e.shiftKey;
        const newSelections = [];

        state.entities.forEach(en => {
            const isContained = en.x >= r.x &&
                en.x + en.w <= r.x + r.w &&
                en.y >= r.y &&
                en.y + en.h <= r.y + r.h;

            if (isContained) {
                newSelections.push(en.id);
            }
        });

        if (isMultiSelect) {
            state.selectedIds = [...new Set([...state.selectedIds, ...newSelections])];
        } else {
            state.selectedIds = newSelections;
        }
        
        state.isSelectingArea = false;
    }
}
"""

# 3. BlockTool.js
block_tool_js = """import { Tool } from './Tool.js';
import { state, saveState } from '../state.js';
import { Entity } from '../entities.js';
import { checkSmartGuides, updateSelectionStats } from '../utils.js';

export class BlockTool extends Tool {
    onMouseDown(e, coords) {
        state.selectedIds = [];
        state.isSelectingArea = true;
        state.selectionBox = { x1: coords.x, y1: coords.y, x2: coords.x, y2: coords.y };
    }

    onMouseMove(e, coords) {
        if (!state.isSelectingArea) return;
        
        state.selectionBox.x2 = coords.x;
        state.selectionBox.y2 = coords.y;

        const sx = state.snapToGrid ? state.gridSizeX : 1;
        const sy = state.snapToGrid ? state.gridSizeY : 1;
        const x = Math.min(state.selectionBox.x1, state.selectionBox.x2);
        const y = Math.min(state.selectionBox.y1, state.selectionBox.y2);
        const w = Math.abs(state.selectionBox.x2 - state.selectionBox.x1);
        const h = Math.abs(state.selectionBox.y2 - state.selectionBox.y1);

        const rect = {
            x: this.snap(x, sx),
            y: this.snap(y, sy),
            w: this.snap(w, sx),
            h: this.snap(h, sy)
        };
        const snapped = checkSmartGuides(rect);
        state.tempRect = { ...rect, x: snapped.x, y: snapped.y };
        updateSelectionStats();
    }

    onMouseUp(e, coords) {
        if (!state.isSelectingArea) return;
        
        if (state.tempRect && state.tempRect.w > 0 && state.tempRect.h > 0) {
            saveState();
            let { x, y, w, h } = state.tempRect;

            if (state.currentTool === 'start') {
                w = state.gridSizeX;
                h = state.gridSizeY;
            }

            state.entities.push(new Entity(state.currentTool, x, y, w, h));
        }
        
        state.isSelectingArea = false;
        state.tempRect = null;
    }
}
"""

# 4. BrushTool.js
brush_tool_js = """import { Tool } from './Tool.js';
import { state, saveState } from '../state.js';
import { Entity } from '../entities.js';
import { optimizeEntities } from '../utils.js';

export class BrushTool extends Tool {
    onMouseDown(e, coords) {
        state.selectedIds = [];
        state.isSelectingArea = true; // flag to allow dragging mouse
        this.manager.didBrushPaint = false;
        
        saveState();
        this.paint(coords);
    }

    onMouseMove(e, coords) {
        if (!state.isSelectingArea) return;
        this.paint(coords);
    }

    onMouseUp(e, coords) {
        if (!state.isSelectingArea) return;
        
        if (this.manager.didBrushPaint) {
            optimizeEntities();
        }
        
        state.isSelectingArea = false;
        this.manager.didBrushPaint = false;
    }
    
    paint(coords) {
        const sx = state.gridSizeX;
        const sy = state.gridSizeY;
        const px = this.snap(coords.x, sx);
        const py = this.snap(coords.y, sy);

        const exists = state.entities.find(en =>
            en.x === px && en.y === py && en.w === sx && en.h === sy && en.type === state.currentTool
        );

        if (!exists) {
            state.entities.push(new Entity(state.currentTool, px, py, sx, sy));
            this.manager.didBrushPaint = true;
        }
    }
}
"""

# We'll save these files
for name, cnt in [('Tool.js', tool_js), ('SelectTool.js', select_tool_js), ('BlockTool.js', block_tool_js), ('BrushTool.js', brush_tool_js)]:
    with open(os.path.join(tools_dir, name), 'w', encoding='utf-8') as f:
        f.write(cnt)

print("Tool files created.")
