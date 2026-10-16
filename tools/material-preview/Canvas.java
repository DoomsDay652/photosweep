package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.awt.image.*;import java.util.ArrayList;
/** Host raster adapter for renderer inspection; this is not an Android emulator. */
public class Canvas {
 public Graphics2D graphics;private final ArrayList<Graphics2D> stack=new ArrayList<>();
 public Canvas(Bitmap bitmap){graphics=bitmap.image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);}
 public int save(){stack.add(graphics);graphics=(Graphics2D)graphics.create();return stack.size();}
 public void restoreToCount(int count){while(stack.size()>=count){graphics.dispose();graphics=stack.remove(stack.size()-1);}}
 public void clipPath(Path p){graphics.clip(p.path);}public void concat(Matrix matrix){graphics.transform(matrix.transform);}public void rotate(float angle,float x,float y){graphics.rotate(Math.toRadians(angle),x,y);}
 private void setup(Paint p){graphics.setComposite(AlphaComposite.SrcOver.derive(p.alpha/255f));graphics.setPaint(p.shader==null?new java.awt.Color(p.color&0xFFFFFF):p.shader.paint);graphics.setStroke(new BasicStroke(Math.max(.01f,p.stroke),1,1));}
 private void shape(java.awt.Shape s,Paint p){setup(p);if(p.style==Paint.Style.FILL)graphics.fill(s);else graphics.draw(s);}
 public void drawPath(Path path,Paint p){shape(path.path,p);}public void drawCircle(float x,float y,float r,Paint p){shape(new Ellipse2D.Float(x-r,y-r,r*2,r*2),p);}public void drawOval(float l,float t,float r,float b,Paint p){shape(new Ellipse2D.Float(l,t,r-l,b-t),p);}public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint p){shape(new RoundRectangle2D.Float(l,t,r-l,b-t,rx*2,ry*2),p);}
 public void drawBitmap(Bitmap b,Rect source,RectF dest,Paint p){setup(p);int l=source==null?0:source.left,t=source==null?0:source.top,r=source==null?b.getWidth():source.right,bot=source==null?b.getHeight():source.bottom;AffineTransform mapping=new AffineTransform();mapping.translate(dest.left,dest.top);mapping.scale((dest.right-dest.left)/(r-l),(dest.bottom-dest.top)/(bot-t));mapping.translate(-l,-t);graphics.drawImage(b.image,mapping,null);}
 public void drawBitmapMesh(Bitmap bitmap,int cols,int rows,float[] verts,int offset,int[] colors,int colorOffset,Paint p){
  setup(p);
  for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
   int a=row*(cols+1)+col,b=a+1,c=a+cols+1,d=c+1;
   triangle(bitmap,cols,rows,verts,offset,colors,colorOffset,a,b,d,p);triangle(bitmap,cols,rows,verts,offset,colors,colorOffset,a,d,c,p);
  }
 }
 private void triangle(Bitmap bitmap,int cols,int rows,float[] v,int vo,int[] colors,int co,int a,int b,int c,Paint p){
  int ca=colors==null?-1:colors[a+co],cb=colors==null?-1:colors[b+co],cc=colors==null?-1:colors[c+co];
  float alpha=((ca>>>24)+(cb>>>24)+(cc>>>24))/765f;if(alpha<=0)return;
  float[] src={a%(cols+1)*bitmap.getWidth()/(float)cols,a/(cols+1)*bitmap.getHeight()/(float)rows,b%(cols+1)*bitmap.getWidth()/(float)cols,b/(cols+1)*bitmap.getHeight()/(float)rows,c%(cols+1)*bitmap.getWidth()/(float)cols,c/(cols+1)*bitmap.getHeight()/(float)rows};
  float[] dst={v[a*2+vo],v[a*2+vo+1],v[b*2+vo],v[b*2+vo+1],v[c*2+vo],v[c*2+vo+1]};Matrix matrix=new Matrix();if(!matrix.setPolyToPoly(src,0,dst,0,3))return;
  int l=Math.max(0,(int)Math.floor(Math.min(src[0],Math.min(src[2],src[4])))),t=Math.max(0,(int)Math.floor(Math.min(src[1],Math.min(src[3],src[5]))));int r=Math.min(bitmap.getWidth(),(int)Math.ceil(Math.max(src[0],Math.max(src[2],src[4])))+1),bottom=Math.min(bitmap.getHeight(),(int)Math.ceil(Math.max(src[1],Math.max(src[3],src[5])))+1);if(r<=l||bottom<=t)return;
  BufferedImage patch=new BufferedImage(r-l,bottom-t,BufferedImage.TYPE_INT_ARGB);Graphics2D pg=patch.createGraphics();pg.drawImage(bitmap.image,-l,-t,null);pg.dispose();
  float red=(((ca>>16)&255)+((cb>>16)&255)+((cc>>16)&255))/765f,green=(((ca>>8)&255)+((cb>>8)&255)+((cc>>8)&255))/765f,blue=((ca&255)+(cb&255)+(cc&255))/765f;
  patch=new RescaleOp(new float[]{red,green,blue,1},new float[4],null).filter(patch,null);
  Graphics2D g=(Graphics2D)graphics.create();g.setComposite(AlphaComposite.SrcOver.derive(alpha*p.alpha/255f));Path2D.Float path=new Path2D.Float();float cx=(dst[0]+dst[2]+dst[4])/3,cy=(dst[1]+dst[3]+dst[5])/3;for(int i=0;i<6;i+=2){float dx=dst[i]-cx,dy=dst[i+1]-cy,len=(float)Math.hypot(dx,dy);float x=dst[i]+.32f*dx/Math.max(.01f,len),y=dst[i+1]+.32f*dy/Math.max(.01f,len);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.closePath();g.clip(path);
  matrix.transform.translate(l,t);g.drawImage(patch,matrix.transform,null);g.dispose();
 }
public void clipRect(float l,float t,float r,float b){graphics.clip(new Rectangle2D.Float(l,t,r-l,b-t));}public int saveLayer(float l,float t,float r,float b,Paint p){return save();}public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}public void drawRect(RectF r,Paint p){drawRect(r.left,r.top,r.right,r.bottom,p);}public void drawLine(float x,float y,float x2,float y2,Paint p){setup(p);graphics.draw(new Line2D.Float(x,y,x2,y2));}}
