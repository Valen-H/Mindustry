package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.ScissorStack;
import io.anuke.arc.input.GestureDetector;
import io.anuke.arc.input.GestureDetector.GestureListener;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.editor.DrawOperation.TileOperation;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.Binding;
import io.anuke.mindustry.ui.GridImage;

import static io.anuke.mindustry.Vars.mobile;
import static io.anuke.mindustry.Vars.ui;

public class MapView extends Element implements GestureListener{
    private MapEditor editor;
    private EditorTool tool = EditorTool.pencil;
    private OperationStack stack = new OperationStack();
    private DrawOperation op;
    private Bresenham2 br = new Bresenham2();
    private boolean updated = false;
    private float offsetx, offsety;
    private float zoom = 1f;
    private boolean grid = false;
    private GridImage image = new GridImage(0, 0);
    private Vector2 vec = new Vector2();
    private Rectangle rect = new Rectangle();
    private Vector2[][] brushPolygons = new Vector2[MapEditor.brushSizes.length][0];

    private boolean drawing;
    private int lastx, lasty;
    private int startx, starty;
    private float mousex, mousey;
    private EditorTool lastTool;

    public MapView(MapEditor editor){
        this.editor = editor;

        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            float size = MapEditor.brushSizes[i];
            brushPolygons[i] = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index, index) <= index - 0.5f);
        }

        Core.input.getInputProcessors().insert(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
        touchable(Touchable.enabled);

        addListener(new InputListener(){

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                mousex = x;
                mousey = y;

                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer != 0){
                    return false;
                }

                if(!mobile && button != KeyCode.MOUSE_LEFT && button != KeyCode.MOUSE_MIDDLE){
                    return true;
                }

                if(button == KeyCode.MOUSE_MIDDLE){
                    lastTool = tool;
                    tool = EditorTool.zoom;
                }

                mousex = x;
                mousey = y;

                op = new DrawOperation(editor.getMap());

                updated = false;

                Point2 p = project(x, y);
                lastx = p.x;
                lasty = p.y;
                startx = p.x;
                starty = p.y;
                tool.touched(editor, p.x, p.y);

                if(tool.edit){
                    updated = true;
                    ui.editor.resetSaved();
                }

                drawing = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!mobile && button != KeyCode.MOUSE_LEFT && button != KeyCode.MOUSE_MIDDLE){
                    return;
                }

                drawing = false;

                Point2 p = project(x, y);

                if(tool == EditorTool.line){
                    ui.editor.resetSaved();
                    Array<Point2> points = br.line(startx, starty, p.x, p.y);
                    for(Point2 point : points){
                        editor.draw(point.x, point.y);
                    }
                    updated = true;
                }

                if(op != null && updated){
                    if(!op.isEmpty()){
                        stack.add(op);
                    }
                    op = null;
                }

                if(lastTool != null){
                    tool = lastTool;
                    lastTool = null;
                }

            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                mousex = x;
                mousey = y;

                Point2 p = project(x, y);

                if(drawing && tool.draggable && !(p.x == lastx && p.y == lasty)){
                    ui.editor.resetSaved();
                    Array<Point2> points = br.line(lastx, lasty, p.x, p.y);
                    for(Point2 point : points){
                        tool.touched(editor, point.x, point.y);
                    }
                    updated = true;
                }
                lastx = p.x;
                lasty = p.y;
            }
        });
    }

    public EditorTool getTool(){
        return tool;
    }

    public void setTool(EditorTool tool){
        this.tool = tool;
    }

    public void clearStack(){
        stack.clear();
    }

    public OperationStack getStack(){
        return stack;
    }

    public boolean isGrid(){
        return grid;
    }

    public void setGrid(boolean grid){
        this.grid = grid;
    }

    public void undo(){
        if(stack.canUndo()){
            stack.undo(editor);
        }
    }

    public void redo(){
        if(stack.canRedo()){
            stack.redo(editor);
        }
    }

    public void addTileOp(TileOperation t){
        op.addOperation(t);
    }

    public boolean checkForDuplicates(short x, short y){
        return op.checkDuplicate(x, y);
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(Core.scene.getKeyboardFocus() == null || !(Core.scene.getKeyboardFocus() instanceof TextField) &&
                !Core.input.keyDown(KeyCode.CONTROL_LEFT)){
            float ax = Core.input.axis(Binding.move_x);
            float ay = Core.input.axis(Binding.move_y);
            offsetx -= ax * 15f / zoom;
            offsety -= ay * 15f / zoom;
        }

        if(ui.editor.hasPane()) return;

        zoom += Core.input.axis(KeyCode.SCROLL) / 10f * zoom;
        clampZoom();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.2f, 20f);
    }

    private Point2 project(float x, float y){
        float ratio = 1f / ((float) editor.getMap().width() / editor.getMap().height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * editor.getMap().width();
        y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * editor.getMap().height();

        if(editor.getDrawBlock().size % 2 == 0 && tool != EditorTool.eraser){
            return Tmp.g1.set((int) (x - 0.5f), (int) (y - 0.5f));
        }else{
            return Tmp.g1.set((int) x, (int) y);
        }
    }

    private Vector2 unproject(int x, int y){
        float ratio = 1f / ((float) editor.getMap().width() / editor.getMap().height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float px = ((float) x / editor.getMap().width()) * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
        float py = ((float) (y) / editor.getMap().height()) * sclheight
                + offsety * zoom - sclheight / 2 + getHeight() / 2;
        return vec.set(px, py);
    }

    @Override
    public void draw(){
        float ratio = 1f / ((float) editor.getMap().width() / editor.getMap().height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float centerx = x + width / 2 + offsetx * zoom;
        float centery = y + height / 2 + offsety * zoom;

        image.setImageSize(editor.getMap().width(), editor.getMap().height());

        if(!ScissorStack.pushScissors(rect.set(x, y, width, height))){
            return;
        }

        Draw.color(Palette.remove);
        Lines.stroke(2f);
        Lines.rect(centerx - sclwidth / 2 - 1, centery - sclheight / 2 - 1, sclwidth + 2, sclheight + 2);
        editor.renderer().draw(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
        Draw.reset();

        if(!ScissorStack.pushScissors(rect.set(x, y, width, height))){
            return;
        }

        if(grid){
            Draw.color(Color.GRAY);
            image.setBounds(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
            image.draw();
            Draw.color();
        }

        int index = 0;
        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            if(editor.getBrushSize() == MapEditor.brushSizes[i]){
                index = i;
                break;
            }
        }

        float scaling = zoom * Math.min(width, height) / editor.getMap().width();

        Draw.color(Palette.accent);
        Lines.stroke(Unit.dp.scl(1f * zoom));

        if(!editor.getDrawBlock().isMultiblock() || tool == EditorTool.eraser){
            if(tool == EditorTool.line && drawing){
                Vector2 v1 = unproject(startx, starty).add(x, y);
                float sx = v1.x, sy = v1.y;
                Vector2 v2 = unproject(lastx, lasty).add(x, y);

                Lines.poly(brushPolygons[index], sx, sy, scaling);
                Lines.poly(brushPolygons[index], v2.x, v2.y, scaling);
            }

            if(tool.edit && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vector2 v = unproject(p.x, p.y).add(x, y);
                Lines.poly(brushPolygons[index], v.x, v.y, scaling);
            }
        }else{
            if((tool.edit || tool == EditorTool.line) && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vector2 v = unproject(p.x, p.y).add(x, y);
                float offset = (editor.getDrawBlock().size % 2 == 0 ? scaling / 2f : 0f);
                Lines.square(
                        v.x + scaling / 2f + offset,
                        v.y + scaling / 2f + offset,
                        scaling * editor.getDrawBlock().size / 2f);
            }
        }

        Draw.color(Palette.accent);
        Lines.stroke(Unit.dp.scl(3f));
        Lines.rect(x, y, width, height);
        Draw.reset();

        ScissorStack.popScissors();
        ScissorStack.popScissors();
    }

    private boolean active(){
        return Core.scene.getKeyboardFocus() != null
                && Core.scene.getKeyboardFocus().isDescendantOf(ui.editor)
                && ui.editor.isShown() && tool == EditorTool.zoom &&
                Core.scene.hit(Core.input.mouse().x, Core.input.mouse().y, true) == this;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        if(!active()) return false;
        offsetx += deltaX / zoom;
        offsety += deltaY / zoom;
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(!active()) return false;
        float nzoom = distance - initialDistance;
        zoom += nzoom / 10000f / Unit.dp.scl(1f) * zoom;
        clampZoom();
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2){
        return false;
    }

    @Override
    public void pinchStop(){

    }
}
