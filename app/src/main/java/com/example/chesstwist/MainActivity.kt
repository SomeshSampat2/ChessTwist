package com.example.chesstwist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.chesstwist.ui.theme.ChessTwistTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

enum class PieceColor {
    WHITE, BLACK
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val position: Pair<Int, Int>,
    val hasMoved: Boolean = false
)

data class ChessMove(
    val from: Pair<Int, Int>,
    val to: Pair<Int, Int>
)

class ChessGame {
    private var pieces = mutableStateListOf<ChessPiece>()
    var currentPlayer by mutableStateOf(PieceColor.WHITE)
    var selectedPiece by mutableStateOf<ChessPiece?>(null)
    var isGameOver by mutableStateOf(false)
    var isCheck by mutableStateOf(false)
    var checkingPiece by mutableStateOf<Pair<Int, Int>?>(null)

    init {
        setupInitialBoard()
    }

    private fun setupInitialBoard() {
        // Setup white pieces
        pieces.add(ChessPiece(PieceType.ROOK, PieceColor.WHITE, Pair(0, 0)))
        pieces.add(ChessPiece(PieceType.KNIGHT, PieceColor.WHITE, Pair(1, 0)))
        pieces.add(ChessPiece(PieceType.BISHOP, PieceColor.WHITE, Pair(2, 0)))
        pieces.add(ChessPiece(PieceType.QUEEN, PieceColor.WHITE, Pair(3, 0)))
        pieces.add(ChessPiece(PieceType.KING, PieceColor.WHITE, Pair(4, 0)))
        pieces.add(ChessPiece(PieceType.BISHOP, PieceColor.WHITE, Pair(5, 0)))
        pieces.add(ChessPiece(PieceType.KNIGHT, PieceColor.WHITE, Pair(6, 0)))
        pieces.add(ChessPiece(PieceType.ROOK, PieceColor.WHITE, Pair(7, 0)))
        for (i in 0..7) {
            pieces.add(ChessPiece(PieceType.PAWN, PieceColor.WHITE, Pair(i, 1)))
        }

        // Setup black pieces
        pieces.add(ChessPiece(PieceType.ROOK, PieceColor.BLACK, Pair(0, 7)))
        pieces.add(ChessPiece(PieceType.KNIGHT, PieceColor.BLACK, Pair(1, 7)))
        pieces.add(ChessPiece(PieceType.BISHOP, PieceColor.BLACK, Pair(2, 7)))
        pieces.add(ChessPiece(PieceType.QUEEN, PieceColor.BLACK, Pair(3, 7)))
        pieces.add(ChessPiece(PieceType.KING, PieceColor.BLACK, Pair(4, 7)))
        pieces.add(ChessPiece(PieceType.BISHOP, PieceColor.BLACK, Pair(5, 7)))
        pieces.add(ChessPiece(PieceType.KNIGHT, PieceColor.BLACK, Pair(6, 7)))
        pieces.add(ChessPiece(PieceType.ROOK, PieceColor.BLACK, Pair(7, 7)))
        for (i in 0..7) {
            pieces.add(ChessPiece(PieceType.PAWN, PieceColor.BLACK, Pair(i, 6)))
        }
    }

    fun getPieceAt(position: Pair<Int, Int>): ChessPiece? {
        return pieces.find { it.position == position }
    }

    fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val piece = getPieceAt(from) ?: return false
        if (piece.color != currentPlayer) return false

        // Check if destination has same color piece
        val destinationPiece = getPieceAt(to)
        if (destinationPiece?.color == piece.color) return false

        // Basic move validation based on piece type
        val isBasicMoveValid = when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(from, to)
            PieceType.ROOK -> isValidRookMove(from, to)
            PieceType.KNIGHT -> isValidKnightMove(from, to)
            PieceType.BISHOP -> isValidBishopMove(from, to)
            PieceType.QUEEN -> isValidQueenMove(from, to)
            PieceType.KING -> isValidKingMove(from, to)
        }

        if (!isBasicMoveValid) return false

        // Test if this move would put or leave own king in check
        val originalPiece = getPieceAt(to)
        val movingPiece = getPieceAt(from)!!

        // Temporarily make the move
        pieces.remove(movingPiece)
        if (originalPiece != null) pieces.remove(originalPiece)
        pieces.add(movingPiece.copy(position = to))

        // Check if king is in check after this move
        val isKingSafe = !isKingInCheck(currentPlayer)

        // Undo the move
        pieces.remove(movingPiece.copy(position = to))
        pieces.add(movingPiece)
        if (originalPiece != null) pieces.add(originalPiece)

        return isKingSafe
    }

    private fun isKingInCheck(color: PieceColor): Boolean {
        val kingPosition = pieces.find { it.type == PieceType.KING && it.color == color }?.position ?: return false
        checkingPiece = null

        pieces.filter { it.color != color }.forEach { piece ->
            val basicMove = when (piece.type) {
                PieceType.PAWN -> isValidPawnMove(piece.position, kingPosition)
                PieceType.ROOK -> isValidRookMove(piece.position, kingPosition)
                PieceType.KNIGHT -> isValidKnightMove(piece.position, kingPosition)
                PieceType.BISHOP -> isValidBishopMove(piece.position, kingPosition)
                PieceType.QUEEN -> isValidQueenMove(piece.position, kingPosition)
                PieceType.KING -> isValidKingMove(piece.position, kingPosition)
            }
            if (basicMove && !isPathBlocked(piece.position, kingPosition)) {
                checkingPiece = piece.position
                return true
            }
        }
        return false
    }

    private fun isCheckmate(color: PieceColor): Boolean {
        // If not in check, it's not checkmate
        if (!isKingInCheck(color)) return false

        // Try all possible moves for all pieces
        pieces.filter { it.color == color }.forEach { piece ->
            for (row in 0..7) {
                for (col in 0..7) {
                    val targetPos = Pair(col, row)
                    if (isValidMove(piece.position, targetPos)) {
                        return false // If any valid move exists, it's not checkmate
                    }
                }
            }
        }
        return true
    }

    private fun isValidPawnMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val piece = getPieceAt(from) ?: return false
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRow = if (piece.color == PieceColor.WHITE) 1 else 6

        // Normal move
        if (from.first == to.first && to.second == from.second + direction && getPieceAt(to) == null) {
            return true
        }

        // Initial two-square move
        if (from.first == to.first && from.second == startRow &&
            to.second == from.second + (2 * direction) &&
            getPieceAt(to) == null &&
            getPieceAt(Pair(from.first, from.second + direction)) == null) {
            return true
        }

        // Capture move
        if (Math.abs(to.first - from.first) == 1 && to.second == from.second + direction) {
            val capturedPiece = getPieceAt(to)
            return capturedPiece != null && capturedPiece.color != piece.color
        }

        return false
    }

    private fun isValidRookMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        if (from.first != to.first && from.second != to.second) return false
        return !isPathBlocked(from, to)
    }

    private fun isValidKnightMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val dx = Math.abs(to.first - from.first)
        val dy = Math.abs(to.second - from.second)
        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2)
    }

    private fun isValidBishopMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        if (Math.abs(to.first - from.first) != Math.abs(to.second - from.second)) return false
        return !isPathBlocked(from, to)
    }

    private fun isValidQueenMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        return isValidRookMove(from, to) || isValidBishopMove(from, to)
    }

    private fun isValidKingMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val dx = Math.abs(to.first - from.first)
        val dy = Math.abs(to.second - from.second)

        // Normal king move
        if (dx <= 1 && dy <= 1) return true

        // Check for castling
        val piece = getPieceAt(from) ?: return false
        if (!piece.hasMoved && dy == 0 && dx == 2) {
            // Determine if it's kingside or queenside castling
            val isKingside = to.first > from.first
            val rookCol = if (isKingside) 7 else 0
            val rookRow = from.second

            // Check if rook is in position and hasn't moved
            val rook = getPieceAt(Pair(rookCol, rookRow))
            if (rook?.type != PieceType.ROOK || rook.color != piece.color || rook.hasMoved) {
                return false
            }

            // Check if path is clear
            val pathStart = minOf(from.first, rookCol)
            val pathEnd = maxOf(from.first, rookCol)
            for (col in pathStart + 1 until pathEnd) {
                if (getPieceAt(Pair(col, rookRow)) != null) {
                    return false
                }
            }

            // Check if king is not in check and doesn't pass through check
            if (isKingInCheck(piece.color)) return false

            // Check squares the king passes through
            val direction = if (isKingside) 1 else -1
            val midSquare = Pair(from.first + direction, from.second)

            // Temporarily move king to check if passing square is safe
            pieces.remove(piece)
            pieces.add(piece.copy(position = midSquare))
            val isMidSquareSafe = !isKingInCheck(piece.color)

            // Restore king position
            pieces.remove(piece.copy(position = midSquare))
            pieces.add(piece)

            return isMidSquareSafe
        }
        return false
    }

    private fun isPathBlocked(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val dx = if (to.first - from.first == 0) 0 else (to.first - from.first) / Math.abs(to.first - from.first)
        val dy = if (to.second - from.second == 0) 0 else (to.second - from.second) / Math.abs(to.second - from.second)

        var x = from.first + dx
        var y = from.second + dy

        while (x != to.first || y != to.second) {
            if (getPieceAt(Pair(x, y)) != null) return true
            x += dx
            y += dy
        }

        return false
    }

    fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val piece = getPieceAt(from) ?: return
        val capturedPiece = getPieceAt(to)

        // Handle castling
        if (piece.type == PieceType.KING && Math.abs(to.first - from.first) == 2) {
            val isKingside = to.first > from.first
            val rookFromCol = if (isKingside) 7 else 0
            val rookToCol = if (isKingside) to.first - 1 else to.first + 1
            val rookRow = from.second

            // Move rook
            val rook = getPieceAt(Pair(rookFromCol, rookRow))!!
            pieces.remove(rook)
            pieces.add(rook.copy(position = Pair(rookToCol, rookRow), hasMoved = true))
        }

        if (capturedPiece != null) {
            pieces.remove(capturedPiece)
        }

        pieces.remove(piece)
        pieces.add(piece.copy(position = to, hasMoved = true))

        currentPlayer = if (currentPlayer == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        checkGameState()
    }

    private fun checkGameState() {
        // Check for check
        isCheck = isKingInCheck(currentPlayer)

        // Check for checkmate
        if (isCheck) {
            isGameOver = isCheckmate(currentPlayer)
        }
    }

    fun promotePawn(position: Pair<Int, Int>, newType: PieceType) {
        val piece = getPieceAt(position) ?: return
        if (piece.type != PieceType.PAWN) return

        pieces.remove(piece)
        pieces.add(ChessPiece(newType, piece.color, position))

        // Check for check/checkmate after promotion
        isCheck = isKingInCheck(currentPlayer)
        if (isCheck) {
            isGameOver = isCheckmate(currentPlayer)
        }
    }

    fun resetGame() {
        pieces.clear()
        currentPlayer = PieceColor.WHITE
        isCheck = false
        isGameOver = false
        checkingPiece = null
        setupInitialBoard()
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessTwistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessBoard()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PawnPromotionDialog(
    position: Pair<Int, Int>,
    currentPlayer: PieceColor,
    onDismiss: () -> Unit,
    onPieceSelected: (PieceType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    PieceType.QUEEN to (if (currentPlayer == PieceColor.WHITE) R.drawable.white_queen else R.drawable.black_queen),
                    PieceType.ROOK to (if (currentPlayer == PieceColor.WHITE) R.drawable.white_rook else R.drawable.black_rook),
                    PieceType.BISHOP to (if (currentPlayer == PieceColor.WHITE) R.drawable.white_bishop else R.drawable.black_bishop),
                    PieceType.KNIGHT to (if (currentPlayer == PieceColor.WHITE) R.drawable.white_knight else R.drawable.black_knight)
                ).forEach { (type, resourceId) ->
                    Image(
                        painter = painterResource(id = resourceId),
                        contentDescription = type.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onPieceSelected(type)
                                onDismiss()
                            }
                    )
                }
            }
        },
        title = { Text("Choose promotion piece") },
        text = { Text("Select a piece to promote your pawn to:") }
    )
}

@Composable
fun ChessBoard() {
    val game = remember { ChessGame() }
    var selectedPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var validMoves by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var showWinAnimation by remember { mutableStateOf(false) }
    var showPromotionDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val infiniteTransition = rememberInfiniteTransition()

    // Background animation for this
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    // Valid move pulse animation
    val validMovePulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    // Win animation scale
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C3E50),
                        Color(0xFF3498DB),
                        Color(0xFF2C3E50)
                    ),
                    start = Offset.Zero,
                    end = Offset(
                        x = cos(gradientAngle * PI.toFloat() / 180) * 1000,
                        y = sin(gradientAngle * PI.toFloat() / 180) * 1000
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (game.isGameOver) {
                    "Checkmate! ${if (game.currentPlayer == PieceColor.WHITE) "Black" else "White"} wins!"
                } else if (game.isCheck) {
                    "${game.currentPlayer} is in check!"
                } else {
                    "${game.currentPlayer}'s turn"
                },
                modifier = Modifier
                    .padding(16.dp)
                    .scale(if (game.isGameOver) scale else 1f)
                    .background(
                        color = Color(0x88000000),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            if (game.isGameOver) {
                Button(
                    onClick = {
                        game.resetGame()
                        selectedPosition = null
                        validMoves = emptyList()
                        showWinAnimation = false
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .scale(scale)
                ) {
                    Text("Play Again")
                }
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .aspectRatio(1f)
                    .fillMaxWidth(0.9f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Column {
                    for (row in 7 downTo 0) {
                        Row {
                            for (col in 0..7) {
                                val position = Pair(col, row)
                                val piece = game.getPieceAt(position)
                                val isSelected = selectedPosition == position
                                val isValidMove = validMoves.contains(position)
                                val isCheck = game.checkingPiece == position

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            if (isCheck) Color(0xFFFF6B6B)
                                            else if ((row + col) % 2 == 1) Color(0xFFF0D9B5)
                                            else Color(0xFFB58863)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color(0xFFFFD700) else Color.Transparent
                                        )
                                        .then(
                                            if (isValidMove) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = Color(0xFF1E88E5).copy(alpha = validMovePulse),
                                                    shape = CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            val clickedPosition = Pair(col, row)
                                            if (validMoves.contains(clickedPosition)) {
                                                // Move the piece
                                                if (selectedPosition != null) {
                                                    val piece = game.getPieceAt(selectedPosition!!)
                                                    if (piece?.type == PieceType.PAWN &&
                                                        (clickedPosition.second == 7 || clickedPosition.second == 0)) {
                                                        showPromotionDialog = clickedPosition
                                                    }
                                                    game.movePiece(selectedPosition!!, clickedPosition)
                                                }
                                                selectedPosition = null
                                                validMoves = emptyList()
                                                if (game.isGameOver) {
                                                    showWinAnimation = true
                                                }
                                            } else {
                                                // Select the piece
                                                val piece = game.getPieceAt(clickedPosition)
                                                if (piece != null && piece.color == game.currentPlayer) {
                                                    selectedPosition = clickedPosition
                                                    validMoves = calculateValidMoves(clickedPosition, game)
                                                } else {
                                                    selectedPosition = null
                                                    validMoves = emptyList()
                                                }
                                            }
                                        }
                                ) {
                                    if (isValidMove) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                                .background(
                                                    color = if (piece != null) Color(0x88FF0000) else Color(0x4400FF00),
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    piece?.let {
                                        val resourceId = when (it.type) {
                                            PieceType.KING -> if (it.color == PieceColor.WHITE) R.drawable.white_king else R.drawable.black_king
                                            PieceType.QUEEN -> if (it.color == PieceColor.WHITE) R.drawable.white_queen else R.drawable.black_queen
                                            PieceType.ROOK -> if (it.color == PieceColor.WHITE) R.drawable.white_rook else R.drawable.black_rook
                                            PieceType.BISHOP -> if (it.color == PieceColor.WHITE) R.drawable.white_bishop else R.drawable.black_bishop
                                            PieceType.KNIGHT -> if (it.color == PieceColor.WHITE) R.drawable.white_knight else R.drawable.black_knight
                                            PieceType.PAWN -> if (it.color == PieceColor.WHITE) R.drawable.white_pawn else R.drawable.black_pawn
                                        }
                                        Image(
                                            painter = painterResource(id = resourceId),
                                            contentDescription = "${it.color} ${it.type}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                                .scale(if (game.isGameOver && it.type == PieceType.KING && it.color == game.currentPlayer) scale else 1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show promotion dialog when needed
    showPromotionDialog?.let { position ->
        PawnPromotionDialog(
            position = position,
            currentPlayer = game.currentPlayer,
            onDismiss = { showPromotionDialog = null },
            onPieceSelected = { pieceType ->
                game.promotePawn(position, pieceType)
                if (game.isGameOver) {
                    showWinAnimation = true
                }
            }
        )
    }
}

fun calculateValidMoves(position: Pair<Int, Int>, game: ChessGame): List<Pair<Int, Int>> {
    val validMoves = mutableListOf<Pair<Int, Int>>()
    val piece = game.getPieceAt(position) ?: return validMoves

    // Check all possible positions on the board
    for (row in 0..7) {
        for (col in 0..7) {
            val targetPos = Pair(col, row)
            if (game.isValidMove(position, targetPos)) {
                validMoves.add(targetPos)
            }
        }
    }
    return validMoves
}