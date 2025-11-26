package soufix.database.active.data;

import com.zaxxer.hikari.HikariDataSource;

import soufix.area.Area;
import soufix.area.SubArea;
import soufix.database.active.AbstractDAO;
import soufix.main.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SubAreaData extends AbstractDAO<SubArea>
{
  public SubAreaData(HikariDataSource dataSource)
  {
    super(dataSource);
  }

  @Override
  public void load(Object obj)
  {
  }

  @Override
  public boolean update(SubArea subarea)
  {
    PreparedStatement p=null;
    try
    {
      p=getPreparedStatement("UPDATE `subarea_data` SET `alignement` = ?, `prisme` = ?, `conquistable` = ? WHERE id = ?");
      p.setInt(1,subarea.getAlignement());
      p.setInt(2,subarea.getPrismId());
      p.setInt(3,subarea.getConquistable() ? 0 : 1);
      p.setInt(4,subarea.getId());
      execute(p);
      return true;
    }
    catch(SQLException e)
    {
      super.sendError("Subarea_dataData update",e);
    } finally
    {
      close(p);
    }
    return false;
  }

  public void load()
  {
    Result result=null;
    try
    {
      result=getData("SELECT * from subarea_data");
      ResultSet RS=result.resultSet;
      while(RS.next())
      {
        final int areaId=RS.getInt("area");
        if(Main.world.getArea(areaId)==null)
        {
          final Area area=new Area(areaId,0);
          Main.world.addArea(area);
          Main.world.logger.warn("Zone manquante détectée: création de l'area {} pour la sous-zone {}",areaId,RS.getInt("id"));
        }

        SubArea SA=new SubArea(RS.getInt("id"),areaId,RS.getString("name"));
        Main.world.addSubArea(SA);
        int alignement=RS.getInt("alignement");
        int conquistable=RS.getInt("conquistable");
        int prisme=RS.getInt("prisme");
        SA.setAlignement(alignement);
        SA.setPrismId(prisme);
        SA.setConquistable(conquistable);
      }
    }
    catch(SQLException e)
    {
      super.sendError("Subarea_dataData load",e);
    } finally
    {
      close(result);
    }
  }
}
