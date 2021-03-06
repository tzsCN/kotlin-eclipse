package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.TextAttribute
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.swt.SWT
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import com.intellij.psi.PsiElement
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.text.rules.Token
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.eclipse.ui.texteditor.AbstractTextEditor

class KotlinTokensFactory(val preferenceStore: IPreferenceStore, val colorManager: IColorManager) {
    val keywordToken = createToken(PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR, PreferenceConstants.EDITOR_JAVA_KEYWORD_BOLD)
    val identifierToken = createToken()
    val stringToken = createToken(PreferenceConstants.EDITOR_STRING_COLOR, PreferenceConstants.EDITOR_STRING_BOLD)
    val singleLineCommentToken = createToken(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR)
    val multiLineCommentToken = createToken(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR)
    val kdocTagNameToken = createToken(boldKey = PreferenceConstants.EDITOR_JAVADOC_KEYWORD_BOLD)
    val whitespaceToken = createToken()

    fun getToken(leafElement: PsiElement): IToken {
        if (leafElement !is LeafPsiElement) return Token.UNDEFINED

        val elementType = leafElement.getElementType()
        return when {
            elementType in KtTokens.KEYWORDS ||
            elementType in KtTokens.SOFT_KEYWORDS ||
            elementType in KtTokens.MODIFIER_KEYWORDS -> keywordToken

            KtTokens.IDENTIFIER == elementType -> identifierToken

            elementType in KtTokens.STRINGS ||
            KtTokens.OPEN_QUOTE == elementType ||
            KtTokens.CLOSING_QUOTE == elementType -> stringToken

            elementType in KtTokens.WHITESPACES -> whitespaceToken

            elementType == KtTokens.EOL_COMMENT -> singleLineCommentToken

            elementType in KtTokens.COMMENTS ||
            elementType in KDocTokens.KDOC_HIGHLIGHT_TOKENS -> multiLineCommentToken

            KDocTokens.TAG_NAME == elementType -> kdocTagNameToken

            else -> Token.UNDEFINED
        }
    }
    
    fun isBlockToken(token: IToken): Boolean {
        return when (token) {
            multiLineCommentToken,
            kdocTagNameToken,
            Token.UNDEFINED -> true
            
            else -> false
        }
    }
    
    private fun createToken(colorKey: String = AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, boldKey: String? = null): Token {
        val color = colorManager.getColor(PreferenceConverter.getColor(preferenceStore, colorKey))
        val style = if (boldKey != null && preferenceStore.getBoolean(boldKey)) SWT.BOLD else SWT.NORMAL
        
        val attribute = TextAttribute(color, null, style)
        return Token(attribute)
    }
}