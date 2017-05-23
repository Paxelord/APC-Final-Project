package proceduralGeneration;

// import java.awt.Point;
import java.awt.*;
import java.util.*;
import java.util.List;

import architecture.Player;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;


public class RoomGenerator
{
    private final static int HashTileGridLength = 4;

    final public Cell[][] cells = new Cell[rows][cols];

    List<Cell> aliveCells = new LinkedList<Cell>();

    static final int rows = 3;

    static final int cols = rows;

    static final Point center = new Point( rows / 2, cols / 2 );


    private void initCells()
    {
        for ( int i = 0; i < cells.length; i++ )
        {
            for ( int j = 0; j < cells[0].length; j++ )
            {
                cells[i][j] = new Cell( i, j );
            }
        }
    }


    public RoomGenerator( List<Point> initAlive )
    {
        initCells();
        for ( Point point : initAlive )
        {
            cells[point.x][point.y].isAlive = true;
        }
    }


    public RoomGenerator()
    {
        this( Arrays.<Point> asList( new Point( center.x - 1, center.y ),
            center,
            new Point( center.x + 1, center.y ) ) );
    }


    /**
     * 
     * Used to decide if a cell is alive
     * 
     * @param numAlive
     * @return
     */
    private boolean simulationRule( int numAlive, Cell currCell )
    {
        if ( numAlive == 3 && !currCell.isAlive )
        {
            return true;
        }
        if ( currCell.isAlive && numAlive <= 6 && numAlive >= 1 )
        {
            return true;
        }
        return false;
    }


    public void update()
    {
        updateFutureRoomCellStates();
        updateQueuedStates();
    }


    public void spawnPlayer( Player player )
    {
        int randomIndex = (int)( Math.random() * aliveCells.size() );
        Cell randomCell = aliveCells.get( randomIndex );
        // TODO: hardcoded constant
        player.moveTo( randomCell.x * 100, randomCell.y * 100 );
    }


    private void updateFutureRoomCellStates()
    {
        for ( int row = 0; row < rows; row++ )
        {
            for ( int col = 0; col < cols; col++ )
            {
                int aliveNeighbors = getNeighborsAlive( row, col );
                cells[row][col].willBeAlive = simulationRule( aliveNeighbors,
                    cells[row][col] );
            }
        }
    }


    public void updateQueuedStates()
    {
        LinkedList<Cell> newAliveCells = new LinkedList<Cell>();
        for ( Cell[] row : cells )
        {
            for ( Cell cell : row )
            {
                cell.isAlive = cell.willBeAlive;
                cell.willBeAlive = null;

                if ( cell.isAlive )
                {
                    newAliveCells.add( cell );
                }
            }
        }
        aliveCells = newAliveCells;
    }


    private int getNeighborsAlive( int x, int y )
    {
        int toRet = 0;
        Iterator<Optional<Cell>> neighbors = getNeighbors( x, y );
        while ( neighbors.hasNext() )
        {
            Optional<Cell> currCell = neighbors.next();
            if ( currCell.isPresent() )
            {
                if ( currCell.get().isAlive )
                {
                    toRet++;
                }
            }

        }
        return toRet;
    }


    private Iterator<Optional<Cell>> getNeighbors( int x, int y )
    {
        ArrayList<Optional<Cell>> ret = new ArrayList<Optional<Cell>>();
        for ( int i = -1; i <= 1; i++ )
        {
            for ( int j = -1; j <= 1; j++ )
            {
                // Don't count current cell
                if ( !( i == 0 && j == 0 ) )
                {
                    int neighborX = x + i;
                    int neighborY = y + j;

                    if ( withinWidth( neighborX ) && withinHeight( neighborY ) )
                    {
                        ret.add( Optional.of( cells[neighborX][neighborY] ) );
                    }
                    else
                    {
                        ret.add( Optional.empty() );
                    }
                }
            }
        }

        return ret.iterator();
    }


    private boolean withinWidth( int toCheck )
    {
        return toCheck >= 0 && toCheck < cols;
    }


    private boolean withinHeight( int toCheck )
    {
        return toCheck >= 0 && toCheck < rows;
    }


    private Optional<Cell> cellAt( int r, int c )
    {
        if ( withinHeight( r ) && withinWidth( c ) )
        {
            return Optional.of( cells[r][c] );
        }
        else
        {
            return Optional.empty();
        }
    }


    public Optional<Cell> above( int r, int c )
    {
        return cellAt( r - 1, c );
    }


    public Optional<Cell> right( int r, int c )
    {
        return cellAt( r, c + 1 );
    }


    public Optional<Cell> below( int r, int c )
    {
        return cellAt( r + 1, c );
    }


    public Optional<Cell> left( int r, int c )
    {
        return cellAt( r, c - 1 );
    }


    public static float roundToLowestMultiple( float toRound, int nearest )
    {
        return (float)( (int)toRound / nearest ) * nearest;
    }


    /**
     * split a list of walls into a hashtable of point keys and walls of the
     * "tile"
     * 
     * @param walls
     * @return
     */
    private Hashtable<Point2D, List<Rectangle>> segment(
        ArrayList<Rectangle> walls,
        int gridLengthByTiles,
        int width )
    {
        Hashtable<Point2D, List<Rectangle>> ret = new Hashtable<>();
        int tileWidth = rows * width / gridLengthByTiles;
        System.out.println( "tile width" + tileWidth );
        for ( Rectangle wall : walls )
        {
            // hella sketch rounding going on here. Bascially round down to the
            // nearest multiple of tileWidth
            float x = roundToLowestMultiple( wall.x, tileWidth );
            float y = roundToLowestMultiple( wall.y, tileWidth );

            Point2D point2D = new Point2D( x, y );

            if ( ret.containsKey( point2D ) )
            {
                ret.get( point2D ).add( wall );
            }
            else
            {
                ArrayList<Rectangle> initWallList = new ArrayList<>();
                initWallList.add( wall );
                ret.put( point2D, initWallList );
            }
        }

        return ret;
    }


    private boolean alive( Optional<Cell> toCheck )
    {
        if ( toCheck.isPresent() )
        {
            return toCheck.get().isAlive;
        }
        else
        {
            return false;
        }
    }


    // length in pixels
    public Hashtable<Point2D, List<Rectangle>> getWalls(
        final int lengthOfCell )
    {
        ArrayList<Rectangle> walls = new ArrayList<Rectangle>();

        // TODO: 1) use foreach 2) include above, below, etc into Cell
        for ( int r = 0; r < rows; r++ )
        {
            for ( int c = 0; c < cols; c++ )
            {
                Cell cell = cells[c][r];
                if ( !cell.isAlive )
                {
                    System.out.println( "adding forbidden cell at:" );
                    System.out.println( cell );
                    walls.add( new Rectangle( cell.x * lengthOfCell,
                        cell.y * lengthOfCell,
                        lengthOfCell,
                        lengthOfCell ) );
                }
            }
        }

        return segment( walls, /* HashTileGridLength */1, lengthOfCell );
    }
}
